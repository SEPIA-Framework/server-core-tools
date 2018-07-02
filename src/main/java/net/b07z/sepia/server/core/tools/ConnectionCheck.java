package net.b07z.sepia.server.core.tools;

import org.json.simple.JSONObject;

/**
 * Test connections e.g. response of HTTP REST endpoints.
 *  
 * @author Florian Quirin
 *
 */
public class ConnectionCheck {
	
	public interface ConnectionCheckAction{
		public void call(int iteration);
	}
	
	/**
	 * Command-line interface.
	 */
	public static void main(String[] args){
		String method = "";
		int maxTries = 10;
		long waitBetween = 2000;
		String expectKey = null;
		String expectValue = null;
		String url = null;
		if (args != null && args.length > 0){
			method = args[0];
			if (args.length > 1){
				for (int i=1; i<args.length; i++){
					if (args[i].startsWith("-maxTries=")){
						maxTries = Integer.parseInt(args[i].replaceFirst(".*?=", "").trim());
					}else if (args[i].startsWith("-waitBetween=")){
						waitBetween = Long.parseLong(args[i].replaceFirst(".*?=", "").trim());
					}else if (args[i].startsWith("-expectKey=")){
						expectKey = args[i].replaceFirst(".*?=", "").trim();
					}else if (args[i].startsWith("-expectValue=")){
						expectValue = args[i].replaceFirst(".*?=", "").trim();
					}else if (args[i].startsWith("-url=")){
						url = args[i].replaceFirst(".*?=", "").trim();
					}
				}
			}
		}
		//call method
		if (method != null && method.equals("httpGetJson") && url != null){
			boolean success = httpGetJson(url, maxTries, waitBetween, expectKey, expectValue, (iteration)->{
				if (iteration <= 1 || (iteration % 20) == 0){
					System.out.println(".");
				}else{
					System.out.print(".");
				}
			});
			if (success){
				System.out.println("SUCCESS");
				System.exit(0);
			}else{
				System.err.println("FAIL");
				System.exit(1);
			}
		}else{
			System.err.println("FAIL");
			System.out.println("\nInvalid parameters. Try\n"
					+ "e.g.: java -jar connection-check.jar httpGetJson -url=http://localhost:20724 -expectKey=result -expectValue=success \n"
					+ "or: java -jar connection-check.jar httpGetJson -url=http://localhost:20724 -maxTries=15 -waitBetween=500");
			System.exit(1);
		}
		
	}
	
	/**
	 * Make a HTTP GET call to URL and optionally check a result key for a certain value.
	 * @param url
	 * @param maxTries
	 * @param waitBetween
	 * @param expectKey
	 * @param expectValue
	 * @param waitAction
	 * @return
	 */
	public static boolean httpGetJson(String url, int maxTries, long waitBetween, String expectKey, String expectValue, ConnectionCheckAction waitAction){
		int tries = 0;
		while(true){
			try{
				JSONObject res = Connectors.httpGET(url);
				//Connection success?
				if (Connectors.httpSuccess(res)){
					//Check key?
					if (expectKey != null && !expectKey.isEmpty()){
						if (expectValue != null){
							//Check value
							String value = JSON.getString(res, expectKey);
							if (value != null && value.equals(expectValue)){
								//Found key and value was good
								return true;
							}
						}else{
							if (res.containsKey(expectKey)){
								//Found key, value does not matter
								return true;
							}
						}
					}else{
						//Connection success
						return true;
					}
				}
			}catch (Exception e){
				//ignore
			}
			tries++;
			if (tries > maxTries)	return false;
			else Timer.threadSleep(waitBetween);
			if (waitAction != null){
				waitAction.call(tries);
			}
		}
	}

}
