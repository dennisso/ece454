import java.util.HashMap;
import java.util.Map;

public class CommandLineArgs {
	String host = "localhost";
	Integer pport;
	Integer mport;
	Integer ncores = Runtime.getRuntime().availableProcessors();
	Map<String, Integer> seeds = new HashMap<String, Integer>();

	public CommandLineArgs(String[] args) {
		for (int i = 0; i < args.length - 1; i++) {
			String s = args[i];
			if (s.equalsIgnoreCase("-host")) {
				host = args[i + 1];
			} else if (s.equalsIgnoreCase("-pport")) {
				pport = Integer.parseInt(args[i + 1]);
			} else if (s.equalsIgnoreCase("-mport")) {
				mport = Integer.parseInt(args[i + 1]);
			} else if (s.equalsIgnoreCase("-ncores")) {
				ncores = Integer.parseInt(args[i + 1]);
			} else if (s.equalsIgnoreCase("-seeds")) {
				String[] seedList = args[i + 1].split("\\W");
				for (int j = 0; j < seedList.length; j = j + 2) {
					seeds.put(seedList[j], Integer.parseInt(seedList[j + 1]));
				}
			}
		}
	}

	public String getHost() {
		return host;
	}

	public Integer getPPort() {
		return pport;
	}

	public Integer getMPort() {
		return mport;
	}

	public Integer getNCores() {
		return ncores;
	}

	public Map<String, Integer> getSeeds() {
		return seeds;
	}

}
