
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import sun.security.pkcs11.wrapper.*;
import static sun.security.pkcs11.wrapper.PKCS11Constants.*;
import sun.security.pkcs11.Secmod.*;

public class VormetricVAEProxy {

	public static final byte[] iv = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10, 0x0A, 0x0B, 0x0C,
			0x0D, 0x0E, 0x0F };
	public static final CK_MECHANISM encMechCbcPad = new CK_MECHANISM(CKM_AES_CBC_PAD, iv);
	public static final CK_MECHANISM encMechCtr = new CK_MECHANISM(CKM_AES_CTR, iv);
	public static final CK_MECHANISM encMechCbc = new CK_MECHANISM(CKM_AES_CBC, iv);

	public static void main(String[] args) {
		DataInputStream is;
		PrintStream os;
		String line = null;
		String pin = "Admin123!";
		String keyName = "vpkcs11_java_test_key";

		if (args != null) {
			System.out.println("Args lenght " + args.length);
			if (args.length == 2) {
				pin = args[0];
				keyName = args[1];
			}
			else{
				System.out.println("usage take two parms pin and key: VormetricVAEProxy snoopy mykeyindsm");
				System.exit(1);
			}
		}

		String raw_text;

		StringBuffer numberPattern = new StringBuffer("0123456789");
		StringBuffer stringPattern = new StringBuffer("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
		StringBuffer combinedPattern = new StringBuffer(
				"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");

		String libPath = null;
		String operation = "CBC_PAD";
		String charSetStr = "0123456789-";
		String charSetInputFile = null;

		byte[] tweak = { 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01, 0x00 };
		CK_MECHANISM encMechFpe = null;
		CK_MECHANISM encMech = null;
		Vpkcs11Session session = Helper.startUp(Helper.getPKCS11LibPath(libPath), pin);
		long keyID = Helper.findKey(session, keyName);

		String inputdata = "app:machine:token:value";

		VormetricVAEProxy vtsc = new VormetricVAEProxy();

		try {
			ServerSocket s = new ServerSocket(8181);
			Socket in = s.accept();

			PrintWriter out = new PrintWriter(in.getOutputStream(), true);
			DataInputStream input;
			try {
				input = new DataInputStream(in.getInputStream());

			} catch (IOException e) {
				System.out.println(e);
			}

			is = new DataInputStream(in.getInputStream());
			os = new PrintStream(in.getOutputStream());
			// As long as we receive data, echo that data back to the client.
			String values[] = null;

			while (true) {
				line = is.readLine();

				System.out.println("line is " + line);
				System.out.println("line length " + line.length());

				values = line.toString().split(":");

				if (values.length < 4) {
					System.out.println("invalid input for " + line);
					System.out.println("proper format is " + inputdata);
					break;
				}

				for (int i = 0; i < values.length; i++) {
					System.out.println("first value " + values[i]);

				}

			//	String function = values[2];
				String datafromclient = values[3];
				operation = values[2];
				

				if (validrequest(values)) {
					if (datafromclient.trim().equalsIgnoreCase("stop")) {
						os.println("stop");
						break;
					} else {

						// String datafromclient = null;

						// String operation1 = null;

						if (operation.equalsIgnoreCase("CBC") || operation.equalsIgnoreCase("CBC_PAD")
								|| operation.equalsIgnoreCase("FPE") || operation.equalsIgnoreCase("CTR")) {
							// Valid operation.
						} else
							operation = "CBC_PAD";

						if (operation.equalsIgnoreCase("CBC") || operation.equalsIgnoreCase("FPE")) {
							if (datafromclient != null && datafromclient.length() > 0) {
								int inputlen = datafromclient.length();
								if (inputlen < 2)
									datafromclient = datafromclient + " ";
							} else {
								datafromclient = "null";
							}

						}

						String input_without_sc = datafromclient.replaceAll(
								"[\\ \\;\\/\\=\\<\\>\\`\\|\\}\\{\\_\\~\\@\\*\\(\\)\\'\\&\\%\\$\\#\\!\\?\\-\\+\\.\\^:,]",
								"");

						System.out.println("result = " + input_without_sc);

						String sc = getSCUnique(datafromclient);
						// int cnt = getSpecialCharacterCount(s);
						System.out.println("sc = " + sc);

						boolean b = isNumeric(input_without_sc);
						if (b) {

							numberPattern.append(sc);
							System.out.println("number pattern = " + numberPattern.toString().trim());
							charSetStr = numberPattern.toString();
						} else {
							b = isAlpha(input_without_sc);
							if (b) {

								stringPattern.append(sc);
								System.out.println("alpha pattern = " + stringPattern.toString().trim());
								charSetStr = stringPattern.toString();
							} else {

								combinedPattern.append(sc);
								System.out.println("combined pattern = " + combinedPattern.toString().trim());
								charSetStr = combinedPattern.toString();
							}
						}

						byte[] plainBytes;
						int plainBytesLen;
						boolean valid_nbr = true;

						System.out.println("Start EncryptDecryptMessage ...");
						int i;

						String plainText, decryptedText = "";

						long nbrofrows = 0;
						if (valid_nbr) {

							if (operation.equals("CTR")) {
								System.out.println("CTR mode selected");
								encMech = encMechCtr;

							} else if (operation.equals("FPE")) {
								System.out.println("FPE mode selected");

								byte[] charSet = charSetStr != null ? charSetStr.getBytes() : "0123456789".getBytes();

								ByteArrayOutputStream fpeIVBytes = new ByteArrayOutputStream(9 + charSet.length);
								DataOutputStream dos = new DataOutputStream(fpeIVBytes);

								try {
									dos.write(tweak, 0, 8);
									dos.write((charSetStr != null ? charSet.length : 1) & 0xFF);
									dos.write(charSet, 0, charSet.length);
									dos.flush();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}

								encMechFpe = new CK_MECHANISM(0x80004001L, fpeIVBytes.toByteArray());

								encMech = encMechFpe;
							} else if (operation.equals("CBC")) {
								System.out.println("CBC mode selected");
								encMech = encMechCbc;
							} else {
								System.out.println("CBC PAD mode selected");
								encMech = encMechCbcPad;
							}

							/* encrypt, decrypt with key */

							plainBytes = datafromclient.getBytes();
							decryptedText = vtsc.encryptDecryptBuf(session, encMech, keyID, plainBytes);

						}

						// String returnval = vtsc.DoIt(datafromclient,
						// function);
						os.println(decryptedText);

					}
				} else {
					System.out.println("invalid request........");
					break;
				}

			}
			System.out.println("Hello on a socket from the server " + line);
			// out.println("Hello on a socket from the server");
			is.close();
			os.close();
			in.close();
		} catch (Exception e) {
		}

	}

	@SuppressWarnings("null")

	static boolean validrequest(String s[]) {
		String applicaiton = s[0];
		String machine = s[1];
		// String classname = s[3];
		System.out.println("in validation for request app ok" + applicaiton + " machine ok" + machine);
		// Make calls to verify request is valid. Can check vormetric VTE
		// protected file for valid reqeust.

		return true;
	}

	public static boolean isNumeric(String str) {
		// StringBuffer specialchar = new StringBuffer();
		for (char c : str.toCharArray()) {

			if (!Character.isDigit(c)) {
				System.out.println(c);
				return false;
			}
		}
		return true;
	}

	public static boolean isAlpha(String str) {

		for (char c : str.toCharArray()) {

			if (!Character.isAlphabetic(c)) {
				System.out.println(c);
				return false;
			}
		}
		return true;

	}

	public static String getSCUnique(String name) {
		StringBuffer returnvalue = new StringBuffer();
		HashMap hm = new HashMap();
		String specialCharacters = " !#$%&'()*+,-./:;<=>?@[]^_`{|}~";
		// String specialCharacters = " !#$%&'()*+,-./:;<=>?@^_`{|}~";
		String str2[] = name.split("");
		int count = 0;
		for (int i = 0; i < str2.length; i++) {
			if (specialCharacters.contains(str2[i])) {
				count++;
				hm.put(str2[i], str2[i]);

			}

		}

		Set set = hm.entrySet();
		Iterator i = set.iterator();

		// Display elements
		while (i.hasNext()) {
			Map.Entry me = (Map.Entry) i.next();
			// System.out.print(me.getKey() + ": ");
			// System.out.println(me.getValue());
			returnvalue.append(me.getKey());
		}

		// System.out.println("spcecial char " + returnvalue.toString());
		return returnvalue.toString();
	}

	public String encryptDecryptBuf(Vpkcs11Session session, CK_MECHANISM encMech, long keyID, byte[] plainBytes) {
		try {
			byte[] encryptedText;
			byte[] decryptedText;
			int encryptedDataLen = 0;
			int decryptedDataLen = 0;
			byte[] outText = {};

			int plainBytesLen = plainBytes.length;
			System.out.println("plaintext byte length: " + plainBytesLen);

			session.p11.C_EncryptInit(session.sessionHandle, encMech, keyID);
			System.out.println("C_EncryptInit success.");

			encryptedDataLen = session.p11.C_Encrypt(session.sessionHandle, plainBytes, 0, plainBytesLen, outText, 0,
					0);
			System.out.println("C_Encrypt success. Encrypted data len = " + encryptedDataLen);

			encryptedText = new byte[encryptedDataLen];
			session.p11.C_Encrypt(session.sessionHandle, plainBytes, 0, plainBytesLen, encryptedText, 0,
					encryptedDataLen);
			System.out.println("C_Encrypt 2nd call succeed. Encrypted data len = " + encryptedDataLen);

			System.out.println("Encrypted Text =  " + new String(encryptedText, 0, encryptedDataLen));
			// encryptedOutFS.write(encryptedText, 0, encryptedDataLen);
			String encryptedTextStr = new String(encryptedText, 0, encryptedDataLen);

			session.p11.C_DecryptInit(session.sessionHandle, encMech, keyID);
			System.out.println("C_DecryptInit success.");

			decryptedDataLen = session.p11.C_Decrypt(session.sessionHandle, encryptedText, 0, encryptedText.length,
					outText, 0, 0);
			System.out.println("C_Decrypt success. Decrypted data length = " + decryptedDataLen);

			decryptedText = new byte[decryptedDataLen];
			decryptedDataLen = session.p11.C_Decrypt(session.sessionHandle, encryptedText, 0, encryptedText.length,
					decryptedText, 0, decryptedDataLen);
			System.out.println("C_Decrypt 2nd call succeed. Decrypted data length = " + decryptedDataLen);

			String decryptedTextStr = new String(decryptedText, 0, decryptedDataLen);
			String plainTextStr = new String(plainBytes);

			System.out.println("Plaintext = " + plainTextStr);
			System.out.println("Decrypted Text New Code = " + decryptedTextStr);

			return encryptedTextStr;

		} catch (PKCS11Exception e) {
			e.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

}