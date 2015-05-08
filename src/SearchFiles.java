import java.util.ArrayList;

//Search for files in list of filenames
public class SearchFiles {
	
	public static String search(ArrayList<String> files, String searchQ)
	{	
		Boolean found = false;
		
		String fileStr = "";
		
		searchQ.trim();
		String srchQs[] = searchQ.split(" ");
		//System.out.println(srchQs[0]);
		ArrayList<String> fileNs; 
		int k;
		
		for(int i=0; i<files.size(); i++)
		{
			String filenames[] = files.get(i).split("_");
			fileNs = new ArrayList<String>();
			for(int j=0; j< filenames.length; j++)
			{
				//System.out.println(filenames[j]);
				fileNs.add(filenames[j].toLowerCase());
			}			
			for(k=0; k<srchQs.length;k++)
			{
				if(!fileNs.contains(srchQs[k].toLowerCase()))
					break;				
			}
			//System.out.println(k);
			//System.out.println(srchQs.length);
			if(k==srchQs.length)
			{
				found=true;
				fileStr = fileStr + " " +  files.get(i);
			}					
		}		
		return fileStr;		
	}
}
