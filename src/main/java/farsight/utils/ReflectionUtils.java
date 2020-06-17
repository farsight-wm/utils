package farsight.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class ReflectionUtils {
	
	private ReflectionUtils() {
		throw new IllegalAccessError();
	}
	
	
	public static Method getMethodIfExistent(Class<?> clazz, String name, Class<?>... parameterTypes) {
		try {
			return clazz.getDeclaredMethod(name, parameterTypes);
		} catch (NoSuchMethodException e) {
			return null;
		}
	}
	
	public static <T> Constructor<T> getConstructorIfExistent(Class<T> clazz, Class<?>... parameterTypes) {
		try {
			return clazz.getDeclaredConstructor(parameterTypes);
		} catch(NoSuchMethodException e) {
			return null;
		}
	}
	
	public static String createCamelcaseName(String prefix, String camalize) {
		if(prefix == null || camalize == null)
			throw new NullPointerException();
		if(prefix.length() == 0 || camalize.length() == 0)
			throw new IllegalArgumentException();
		
		return prefix + camalize.substring(0, 1).toUpperCase() + camalize.substring(1);
	}
	
	public static boolean isStatic(Member member) {
		return Modifier.isStatic(member.getModifiers());
	}
	
	public static Object doInvoke(Method method, Object target, Object... args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if(!method.isAccessible())
			method.setAccessible(true);
		return method.invoke(target, args);
	}
	
	public static Object getStaticFieldIfExistent(String name, Class<?> type) throws IllegalAccessException {
		Field field;
		try {
			field = type.getDeclaredField(name);
			if(!field.isAccessible())
				field.setAccessible(true);
			if(Modifier.isStatic(field.getModifiers()))
				return field.get(null);
		} catch (NoSuchFieldException e) {
		}
		return null;
	}
	
	public static Object getFieldIfExistent(String name, Object target) throws IllegalAccessException, SecurityException {
		Field field;
		try {
			field = target.getClass().getDeclaredField(name);
		} catch (NoSuchFieldException e) {
			return null;
		}
		if(!field.isAccessible())
			field.setAccessible(true);
		return field.get(target);
	}


	public static <T> T createInstance(Class<T> type, Object... args) throws NoSuchMethodException, SecurityException,
			InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Class<?>[] argTypes = new Class<?>[args.length];
		for(int i = 0; i < args.length; i++) {
			argTypes[i] = args[i].getClass();
		}
		Constructor<T> constructor = type.getDeclaredConstructor(argTypes);
		return constructor.newInstance(args);
	}
	
	

}
