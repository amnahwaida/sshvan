import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.io.File;

public class InspectZTSocket {
    public static void main(String[] args) throws Exception {
        File file = new File("classes.jar");
        URL url = file.toURI().toURL();
        URLClassLoader classLoader = new URLClassLoader(new URL[]{url});
        Class<?> clazz = classLoader.loadClass("com.zerotier.sockets.ZeroTierSocket");
        
        System.out.println("Constructors:");
        for (Constructor<?> c : clazz.getConstructors()) {
            System.out.println(c);
        }
        System.out.println("Methods:");
        for (Method m : clazz.getDeclaredMethods()) {
            System.out.println(m);
        }
    }
}
