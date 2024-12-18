package was.httpserver.servlet.annotation;

import was.httpserver.HttpRequest;
import was.httpserver.HttpResponse;
import was.httpserver.HttpServlet;
import was.httpserver.PageNotFoundException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class AnnotationServletV2 implements HttpServlet {
    private final List<Object> controllers;

    public AnnotationServletV2(List<Object> controllers) {
        this.controllers = controllers;
    }
    @Override
    public void service(HttpRequest request, HttpResponse response) throws IOException {
        String path = request.getPath();
        for (Object controller : controllers) {
            Class<?> aClass = controller.getClass();
            Method[] declaredMethods = aClass.getDeclaredMethods();
            for (Method declaredMethod : declaredMethods) {
                Mapping annotation = declaredMethod.getAnnotation(Mapping.class);
                if(annotation == null) continue;
                if (path.equals(annotation.value())){
                    invoke(controller, declaredMethod, request, response);
                    return;
                }
            }
        }
        throw new PageNotFoundException("request = " + request);
    }

    private static void invoke(Object controller, Method declaredMethod, HttpRequest request, HttpResponse response) {

        Class<?>[] parameterTypes = declaredMethod.getParameterTypes();
        Object[] args = new Object[parameterTypes.length];

        for (int i = 0; i< parameterTypes.length; i++){
            if(parameterTypes[i] == HttpRequest.class){
                args[i] = request;
            }else if(parameterTypes[i] == HttpResponse.class){
                args[i] = response;
            }else{
                throw new IllegalArgumentException("Unsupported parameter type: " + parameterTypes[i]);
            }
        }

        try {
            declaredMethod.invoke(controller, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
