import java.net.UnknownHostException;
import java.util.ArrayList;

//To store child node info
public class ChildNodeInfo {
	
	Node childNode;
	Integer hops;

	public ChildNodeInfo(Node node, Integer hop) throws UnknownHostException
	{
		childNode = new Node();
		this.childNode.nodeIP = node.nodeIP;
		this.childNode.nodePort = node.nodePort;
		this.childNode.num_files = node.num_files;
		this.childNode.filenames = node.filenames;
		this.hops = hop;
	}
}
