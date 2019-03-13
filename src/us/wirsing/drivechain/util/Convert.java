package us.wirsing.drivechain.util;

import javax.xml.bind.DatatypeConverter;
import java.util.ArrayList;
import java.util.List;

public class Convert {

	private Convert() {
	}

	public static String bytes2hex(byte[] bytes) {
		return DatatypeConverter.printHexBinary(bytes);
	}

	public static List<Byte> array2list(byte[] bytes) {
		List<Byte> ret = new ArrayList<Byte>(bytes.length);
		for (byte b : bytes) {
			ret.add(b);
		}
		return ret;
	}
}
