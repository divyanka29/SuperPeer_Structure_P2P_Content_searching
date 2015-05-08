import java.util.ArrayList;

public class AuxFunctions {

	public static String getMsg(String rawMsg) {
		Integer lenMsg = rawMsg.length() + 4;
		String len = "0000" + lenMsg.toString();
		String len1 = len.substring(len.length() - 4, len.length());
		String finalMsg = len1 + rawMsg;

		return finalMsg;
	}

	public static ArrayList<Node> removeNode(ArrayList<Node> nodes, String IP, Integer Port) {
		// System.out.println(IP +":"+ Port);
		String check = IP + ":" + Port;
		// System.out.println(check.length());
		String IP_Port;
		for (int i = 0; i < nodes.size(); i++) {
			IP_Port = nodes.get(i).nodeIP + ":" + nodes.get(i).nodePort;
			IP_Port = IP_Port.trim();

			if (IP_Port.equalsIgnoreCase(check)) {
				nodes.remove(i);
			}

		}

		return nodes;

	}

	public static ArrayList<Node> removeDuplicateNode(ArrayList<Node> nodes, String IP, Integer Port) {
		String check;
		String IP_Port;
		for (int i = 0; i < nodes.size(); i++) {
			check = new String(nodes.get(i).nodeIP + ":" + nodes.get(i).nodePort).trim();
			for (int j = i + 1; j < nodes.size(); j++) {
				IP_Port = new String(nodes.get(j).nodeIP + ":" + nodes.get(j).nodePort).trim();
				if (IP_Port.equalsIgnoreCase(check)) {
					nodes.remove(j);
					j--;
				}
			}
		}

		return nodes;

	}

	public static ArrayList<ChildNodeInfo> removeChildNode(ArrayList<ChildNodeInfo> nodes, String IP, Integer Port) {
		String check = new String(IP + ":" + Port).trim();
		String IP_Port;
		for (int i = 0; i < nodes.size(); i++) {
			IP_Port = new String(nodes.get(i).childNode.nodeIP + ":" + nodes.get(i).childNode.nodePort).trim();
			if (IP_Port.equalsIgnoreCase(check)) {
				nodes.remove(i);
			}
		}

		return nodes;
	}

	public static Boolean checkIfSelfNode(String IP_self, Integer Port_self, String IP, Integer Port) {
		String check = new String(IP + ":" + Port).trim();
		String IP_Port = new String(IP_self + ":" + Port_self).trim();

		if (IP_Port.equalsIgnoreCase(check))
			return true;
		else
			return false;
	}
}
