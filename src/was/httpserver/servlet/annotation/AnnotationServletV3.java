package was.httpserver.servlet.annotation;

import was.httpserver.HttpRequest;
import was.httpserver.HttpResponse;
import was.httpserver.HttpServlet;
import was.httpserver.PageNotFoundException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnnotationServletV3 implements HttpServlet {
    private final Map<String, ControllerMethod> pathMap;

    public AnnotationServletV3(List<Object> controllers) {
        this.pathMap = new HashMap<>();
        initializePathMap(controllers);
    }

    @Override
    public void service(HttpRequest request, HttpResponse response) throws IOException {
        String path = request.getPath();
        ControllerMethod controllerMethod = pathMap.get(path);
        if (controllerMethod == null) throw new PageNotFoundException("request = " + request);
        controllerMethod.invoke(request, response);
    }

    private void initializePathMap(List<Object> controllers) {
        for (Object controller : controllers) {
            for (Method declaredMethod : controller.getClass().getDeclaredMethods()) {
                if (declaredMethod.isAnnotationPresent(Mapping.class)) {
                    String value = declaredMethod.getAnnotation(Mapping.class).value();
                    if (pathMap.containsKey(value)) {
                        ControllerMethod controllerMethod = pathMap.get(value);
                        throw new IllegalArgumentException("경로 중복 등록, path=" + value + ", method=" + declaredMethod + ", 이미 등록된 메소드=" + controllerMethod.method);
                    }
                    pathMap.put(value, new ControllerMethod(controller, declaredMethod));
                }
            }
        }
    }

    private static class ControllerMethod {
        private final Object controller;
        private final Method method;

        private ControllerMethod(Object controller, Method declaredMethod) {
            this.method = declaredMethod;
            this.controller = controller;
        }

        private void invoke(HttpRequest request, HttpResponse response) {

            Class<?>[] parameterTypes = method.getParameterTypes();
            Object[] args = new Object[parameterTypes.length];

            for (int i = 0; i < parameterTypes.length; i++) {
                if (parameterTypes[i] == HttpRequest.class) {
                    args[i] = request;
                } else if (parameterTypes[i] == HttpResponse.class) {
                    args[i] = response;
                } else {
                    throw new IllegalArgumentException("Unsupported parameter type: " + parameterTypes[i]);
                }
            }

            try {
                method.invoke(controller, args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
