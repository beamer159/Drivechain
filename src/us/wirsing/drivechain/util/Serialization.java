package us.wirsing.drivechain.util;

import java.io.*;

public class Serialization {

	private Serialization() {
	}

	public static <T extends Serializable> byte[] serialize(T t) {
		ByteArrayOutputStream osBytes = new ByteArrayOutputStream();
		try {
			ObjectOutputStream osObject = new ObjectOutputStream(osBytes);
			osObject.writeObject(t);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return osBytes.toByteArray();
	}

	public static <T extends Serializable> T deserialize(byte[] bytes) {
		T ret = null;
		try {
			ObjectInputStream isObject = new ObjectInputStream(new ByteArrayInputStream(bytes));
			ret = (T)isObject.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}

		return ret;
	}
}
