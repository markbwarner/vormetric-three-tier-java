import java.io.*;
import java.net.*;

import org.apache.commons.codec.binary.Base64;
import com.jayway.jsonpath.*;
import javax.net.ssl.HttpsURLConnection;

public class VormetricVTSProxy {

	String trustedstoredefaultlocation = "/tmp/mytrustedvtskeystore";
	String vtshostip = "192.168.159.141";
	String user = "vtsroot";

	public static void main(String[] args) {
		DataInputStream is;
		PrintStream os;
		String line = null;

		String inputdata = "app:machine:token:value";

		VormetricVTSProxy vtsc = new VormetricVTSProxy();

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

				String function = values[2];
				String datafromclient = values[3];

				if (validrequest(values)) {
					if (datafromclient.trim().equalsIgnoreCase("stop")) {
						os.println("stop");
						break;
					} else {

						String returnval = vtsc.DoIt(datafromclient, function);
						os.println(returnval);

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
	private String DoIt(String inputfromclient, String function) {
		String userpwd = user + ":Vormetric123!";
		String credential = Base64.encodeBase64String(userpwd.getBytes());

		String token = null;

		// Two options to get this example to work:
		// Must export a certificate from the VTS server and then import to a
		// keystore.
		// Put this as VM arguments in the run configuration
		// -Djavax.net.debug=ssl
		// -Djavax.net.ssl.trustStore="C:\\tmp\\mytrustedvtskeystore"
		System.setProperty("javax.net.ssl.trustStore", trustedstoredefaultlocation);
		// System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
		// System.setProperty("javax.net.debug", "ssl");

		try {

			// Also needed to add this code as well in order for it to work.
			// Note ip must be = to VTS IP.

			javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(new javax.net.ssl.HostnameVerifier() {

				public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {
					return hostname.equals(vtshostip);
				}
			});

			// Tokenize request
			String https_url = null;

			String jStr = null;
			HttpsURLConnection con = null;
			if (function.equalsIgnoreCase("token")) {

				https_url = "https://" + vtshostip + "/vts/rest/v2.0/tokenize/";
				URL myurl = new URL(https_url);
				 con = (HttpsURLConnection) myurl.openConnection();

				jStr = "{\"data\":\"" + inputfromclient + "\",\"tokengroup\":\"t1\",\"tokentemplate\":\"Credit Card\"}";
			} else {

				https_url = "https://" + vtshostip + "/vts/rest/v2.0/detokenize/";
				URL myurl = new URL(https_url);
				 con = (HttpsURLConnection) myurl.openConnection();
				jStr = "{\"token\":\"" + inputfromclient
						+ "\",\"tokengroup\":\"t1\",\"tokentemplate\":\"Credit Card\"}";

			}

			con.setRequestProperty("Content-length", String.valueOf(jStr.length()));
			con.setRequestProperty("Content-Type", "application/json");
			con.setRequestProperty("Authorization", "Basic " + credential);
			con.setRequestMethod("POST");
			con.setDoOutput(true);
			con.setDoInput(true);

			DataOutputStream output = new DataOutputStream(con.getOutputStream());
			output.writeBytes(jStr);
			output.close();
			BufferedReader rd = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String line = "";
			String strResponse = "";
			while ((line = rd.readLine()) != null) {
				strResponse = strResponse + line;
			}
			rd.close();

			// Also added better error checking...
System.out.println("response is.... " + strResponse);
			if (JsonPath.read(strResponse, "$.status").toString().equals("error")) {
				System.out.println("Error here is the return: " + strResponse);

			} else {
				if (function.equalsIgnoreCase("token")) {
					token = JsonPath.read(strResponse, "$.token").toString();
				} else {
					token = JsonPath.read(strResponse, "$.data").toString();
				}
				con.disconnect();
				// System.out.println("Tokenize server: " + https_url);
				System.out.println("Tokenize request: " + jStr);
				System.out.println("Tokenize response: " + strResponse);
			}
			// Bulk tokenize
			// Detokenize request

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return token;
	}

	static boolean validrequest(String s[]) {
		String applicaiton = s[0];
		String machine = s[1];
		// String classname = s[3];
		System.out.println("in validation for request app ok" + applicaiton + " machine ok" + machine);
		// Make calls to verify request is valid. Can check vormetric VTE
		// protected file for valid reqeust.

		return true;
	}

}