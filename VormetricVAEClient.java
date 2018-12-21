import java.io.*;
import java.net.*;
import java.util.Random;

public class VormetricVAEClient {

	static String application = "hr";
	String inputdata = "app:machine:operation:value";

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

	public static void main(String[] args) {

		try {
			Integer loopcnt = new Integer(1);
			String ip = "192.168.159.134";
			String algo = "CTR";
			if (args.length == 3) {
				if (isNumeric(args[1]))
				{
					ip = args[0];
					loopcnt = new Integer(args[1]);
					algo = args[2];
					if (algo.equalsIgnoreCase("CTR") || algo.equalsIgnoreCase("CBC_PAD")|| algo.equalsIgnoreCase("CBC")|| algo.equalsIgnoreCase("FPE"))
					System.out.println("Starting client with ip " + ip + " loopcnter " + loopcnt);
					else
						System.out.println("invalid algorithum valid values are CTR, CBC_PAD, CBC ");
				}
				else {
					System.out.println("Loop counter must be numeric ");
					System.exit(1);
				}
			} else {
				System.out.println("format is program name ipaddress loopcnt algorithum ");
				System.exit(1);
			}
			Socket s = new Socket(args.length == 0 ? "192.168.159.134" : args[0], 8181);
		//	Socket s = new Socket(args.length == 0 ? "192.168.159.134" : args[0], 8181);
			DataOutputStream os = null;
			DataInputStream is = null;
			os = new DataOutputStream(s.getOutputStream());
			is = new DataInputStream(s.getInputStream());

			Random r = new Random();
			int Low = 1000000;
			int High = 3000000;
			
			
			 
			
			if (s != null && os != null && is != null) {
				// "5471949763376677", "5545127221796024",
				// The capital string before each colon has a special meaning to
				// SMTP
				// you may want to read the SMTP specification, RFC1822/3
				StringBuffer sb = null;
				
				String hostname = getHostName();
			//	StringBuffer randomestr = new StringBuffer();
				int Result = 0;
				
				for (int i = 0; i < loopcnt; i++) {
				 Result = r.nextInt(High-Low) + Low;
					System.out.println("randome number " + Result);
					
					sb = new StringBuffer();
					sb.append(application);
					sb.append(":");
					
					sb.append(hostname);
					sb.append(":");
					sb.append(algo);
					sb.append(":");
					sb.append(String.valueOf(Result));
					//randomestr.append(String.valueOf(Result));
					sb.append(" \n");
					os.writeBytes(sb.toString());
					System.out.println(sb.toString());
					// os.writeBytes("5471949763376677 \n");
					
		/*			sb = new StringBuffer();
					sb.append(application);
					sb.append(":");

					 
					sb.append(hostname);
					sb.append(":");
					sb.append("CBC_PAD");
					sb.append(":");
					sb.append("5545127221796024 \n");
					os.writeBytes(sb.toString());

					sb = new StringBuffer();
					sb.append(application);
					sb.append(":");

					 
					sb.append(hostname);
					sb.append(":");
					sb.append("CBC_PAD");
					sb.append(":");
					sb.append("7381572023856677 \n");
					os.writeBytes(sb.toString());

					sb = new StringBuffer();
					// os.writeBytes("5545127221796024 \n");
					// os.writeBytes("\n.\n");
					// os.writeBytes("stop \n");
					sb.append(application);
					sb.append(":");
				 
					sb.append(hostname);
					sb.append(":");
					sb.append("CBC_PAD");
					sb.append(":");
					sb.append("0828428305336024 \n");
					os.writeBytes(sb.toString());
*/


				}
				
				sb = new StringBuffer();
				// os.writeBytes("5545127221796024 \n");
				// os.writeBytes("\n.\n");
				// os.writeBytes("stop \n");
				sb.append(application);
				sb.append(":");
				hostname = getHostName();
				sb.append(hostname);
				sb.append(":");
				sb.append("CBC_PAD");
				sb.append(":");
				sb.append("stop \n");
				os.writeBytes(sb.toString());
				

			}

			OutputStream out = s.getOutputStream();
			BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			String responseLine;
			while ((responseLine = is.readLine()) != null) {
				System.out.println("Server: " + responseLine);
				if (responseLine.indexOf("stop") != -1) {
					break;
				}
			}

			// String str = in.readLine();
			// System.out.println("Socket message: " + str);
			in.close();
			os.close();
			is.close();
			s.close();

		} catch (Exception e) {
		}
	}

	public static String getHostName() throws UnknownHostException {
		InetAddress iAddress = InetAddress.getLocalHost();
		String hostName = iAddress.getHostName();
		// To get the Canonical host name
		String canonicalHostName = iAddress.getCanonicalHostName();

		System.out.println("HostName:" + hostName);
		System.out.println("Canonical Host Name:" + canonicalHostName);
		return canonicalHostName;

	}

}