package org.carlspring.strongbox.web;

import org.carlspring.strongbox.configuration.Configuration;
import org.carlspring.strongbox.configuration.ConfigurationManager;
import org.carlspring.strongbox.storage.Storage;
import org.carlspring.strongbox.storage.repository.Repository;
import org.carlspring.strongbox.validation.ElementNotFoundException;
import org.carlspring.strongbox.validation.ServiceUnavailableException;

import javax.inject.Inject;
import java.util.Map;

import liquibase.util.StringUtils;
import org.apache.commons.collections.MapUtils;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

/**
 * @author Pablo Tirado
 */
public class RepositoryInstanceMethodArgumentResolver
        implements HandlerMethodArgumentResolver
{

    static final String NOT_FOUND_STORAGE_MESSAGE = "Could not find requested storage %s.";
    static final String NOT_FOUND_REPOSITORY_MESSAGE = "Could not find requested repository %s:%s.";
    static final String NOT_IN_SERVICE_REPOSITORY_MESSAGE = "Requested repository %s:%s is out of service.";

    @Inject
    protected ConfigurationManager configurationManager;

    @Override
    public boolean supportsParameter(final MethodParameter parameter)
    {

        // Check parameter annotation type
        if (!parameter.hasParameterAnnotation(RepositoryMapping.class))
        {
            return false;
        }
        // Check parameter type.
        return parameter.getParameterType().equals(Repository.class);
    }

    @Override
    public Object resolveArgument(final MethodParameter parameter,
                                  final ModelAndViewContainer modelAndViewContainer,
                                  final NativeWebRequest nativeWebRequest,
                                  final WebDataBinderFactory webDataBinderFactory)
            throws MissingPathVariableException
    {
        final String storageId = validateAndGetPathVariable(parameter, nativeWebRequest, "storageId");

        final Storage storage = getStorage(storageId);
        if (storage == null)
        {
            final String message = String.format(NOT_FOUND_STORAGE_MESSAGE, storageId);
            throw new ElementNotFoundException(message);
        }

        final String repositoryId = validateAndGetPathVariable(parameter, nativeWebRequest, "repositoryId");
        final Repository repository = getRepository(storageId, repositoryId);
        if (repository == null)
        {
            final String message = String.format(NOT_FOUND_REPOSITORY_MESSAGE, storageId, repositoryId);
            throw new ElementNotFoundException(message);
        }

        final boolean inService = repository.isInService();
        if (!inService)
        {
            final String message = String.format(NOT_IN_SERVICE_REPOSITORY_MESSAGE, storageId, repositoryId);
            throw new ServiceUnavailableException(message);
        }

        return repository;
    }

    private String validateAndGetPathVariable(final MethodParameter parameter,
                                              final NativeWebRequest nativeWebRequest,
                                              final String pathVariableName)
            throws MissingPathVariableException
    {
        @SuppressWarnings("unchecked")
        final Map<String, String> uriTemplateVars = (Map<String, String>) nativeWebRequest.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (MapUtils.isNotEmpty(uriTemplateVars))
        {
            final String pathVariable = uriTemplateVars.get(pathVariableName);
            if (StringUtils.isNotEmpty(pathVariable))
            {
                return pathVariable;
            }
        }

        throw new MissingPathVariableException(pathVariableName, parameter);
    }

    private Storage getStorage(final String storageId)
    {
        final Configuration configuration = configurationManager.getConfiguration();
        if (configuration == null)
        {
            return null;
        }
        return configuration.getStorage(storageId);
    }

    private Repository getRepository(final String storageId,
                                     final String repositoryId)
    {
        final Storage storage = getStorage(storageId);
        if (storage == null)
        {
            return null;
        }
        return storage.getRepository(repositoryId);
    }

}
