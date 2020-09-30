import java.awt.List;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.StringTokenizer;
import java.util.Vector;


public class WebSearch 
{
	static String directoryName;
	static String searchStrategyName;
	
	//List to store nodes that have been closed & open.
	static HashSet<String> CLOSED;
	static HashSet<String> visitedLinks;
	
	//This is a priority Q for implementing the Fringe for all cases.
	static PriorityQueue<SearchNode> OPEN;
	
	static final boolean DEBUGGING = false; // When set, report what's happening.

	static int beamWidth = 2; // If searchStrategy = "beam",
	// limit the size of OPEN to this value.
	// The setSize() method in the Vector
	// class can be used to accomplish this.

	static final String START_NODE     = "page1.html";

	//This patters is used for determining if a given page is GOAL node.
	static final String GOAL_PATTERN   = "QUERY1 QUERY2 QUERY3 QUERY4";

	
	public static void main(String args[])
	{ 
		if (args.length != 2)
		{
			System.out.println("You must provide the directoryName and searchStrategyName.  Please try again.");
		}
		else
		{
			directoryName = args[0]; // Read the directory name from where to pick the web pages.
			searchStrategyName = args[1]; // Read the search strategy to use.
			
			if (searchStrategyName.equalsIgnoreCase("breadth") ||
					searchStrategyName.equalsIgnoreCase("depth")   ||
					searchStrategyName.equalsIgnoreCase("best")    ||
					searchStrategyName.equalsIgnoreCase("beam"))
			{
				performSearch(START_NODE, directoryName, searchStrategyName);
			}
			else
			{
				System.out.println("The valid search strategies are:");
				System.out.println("  BREADTH DEPTH BEST BEAM");
			}
		}
		
		Utilities.waitHere("Press ENTER to exit.");
	}

	/*	Method Name : performSearch
		Use : This method initiates the Priority Q structure bases on searchStrategy and iterates
			over the fringe to pick out elements based on their relative priority.
	*/
	static void performSearch(String startNode, String directoryName, String searchStrategy)
	{
		int nodesVisited = 0;
		
		//OPEN_ELEMENTS = new HashSet<String>();
		visitedLinks = new HashSet<String>();
		
		/*Base on search strategy, we will decide the strcuture of the Priority Q.
		 * For Breadth First, we will give elements with higher timestamp a lower priority.
		 * For  Depth First, we will give elements with higher timestamp a higher priority
		 * For Best First, we will calculate the Hueristic value and assign this value as the priority*/
		if(searchStrategy.compareToIgnoreCase("breadth") == 0){
			OPEN = 	new PriorityQueue<SearchNode>(new BfsQComparator());
		}else if(searchStrategy.compareToIgnoreCase("depth") == 0){
			OPEN = 	new PriorityQueue<SearchNode>(new DfsQComparator());
		}else if(searchStrategy.compareToIgnoreCase("best") == 0){
			OPEN = 	new PriorityQueue<SearchNode>(new BestQComparator());
		}else if(searchStrategy.compareToIgnoreCase("beam") == 0){
			//To be implemented
		} 
		
		CLOSED = new HashSet<String>();

		SearchNode startSearchNode  = new SearchNode(startNode);
		
		//Here, we are not assigning priority to start node as this will be popped out of the queue before anything else.
		OPEN.add(startSearchNode);
		visitedLinks.add(startNode);
		
		//OPEN_ELEMENTS.add(startNode);

		while (!OPEN.isEmpty())
		{
			SearchNode currentNode = pop();
			/*if(OPEN_ELEMENTS.contains(currentNode.nodeName)){
				OPEN_ELEMENTS.remove(currentNode.nodeName);
			}*/
			
			String currentURL = currentNode.getNodeName();
			nodesVisited++;

			// Go and fetch the contents of this file.
			String contents = Utilities.getFileContents(directoryName
					+ File.separator
					+ currentURL);

			//If the current node is a goal node, print out the solution path there along with the solution path length
			if (isaGoalNode(contents))
			{
				int pathLen = currentNode.reportSolutionPath();
				System.out.println("Solution length: [" + pathLen + "]");
				break;
			}

			//Remember this node was visited.
			CLOSED.add(currentURL);

			addNewChildrenToOPEN(currentNode, contents, searchStrategy);

			// Provide a status report.
			if (DEBUGGING) System.out.println("Nodes visited = " + nodesVisited
					+ " |OPEN| = " + OPEN.size());
		}

		System.out.println(" Visited " + nodesVisited + " nodes, starting @" +
				" " + directoryName + File.separator + startNode +
				", using: " + searchStrategy + " search.");
	}

	// This method reads the page's contents and
	// collects the 'children' nodes (ie, the hyperlinks on this page).
	// The parent node is also passed in so that 'backpointers' can be
	// created (in order to later extract solution paths).
	static void addNewChildrenToOPEN(SearchNode parent, String contents, String searchStrategy)
	{
		StringTokenizer st = new StringTokenizer(contents);
		
		/*This HashMap will be created for when we find all children links for the current page.
		This will store and update the priorities of all hyperlinks on the given page.
		Once we done with parsing the entire page, we will add non-duplicate elements from this map
		into our global priority queue*/
		HashMap<String, Double> childPriorities = new HashMap<String, Double>();
		
		//This is just a list storing all children of the current page.
		ArrayList<String> children = new ArrayList<String>();
		
		double h1Val = 0.0; //For keeping track of 1st Huerisitic ie. the more Query words in page, the more the HVal for the hyperlinks in page.
		
		
		while (st.hasMoreTokens())
		{ 
			String token = st.nextToken();

			//Look for Queries on the given page. For each query, update the h1Val.
			if(searchStrategy.compareToIgnoreCase("best") == 0){
				if(token.contains("query")){
					h1Val += 1;
				}
			}
		
			int qOccurance;//This will compute the H2/H3 values for given hypertexts
			
			// Look for the hyperlinks on the current page.
			// At the start of some hypertext?  Otherwise, ignore this token.
			if (token.equalsIgnoreCase("<A"))
			{
				String hyperlink; // The name of the child node.

				if (DEBUGGING) System.out.println("Encountered a HYPERLINK");

				// Read: HREF = page#.html >

				token = st.nextToken();
				if (!token.equalsIgnoreCase("HREF"))
				{
					System.out.println("Expecting 'HREF' and got: " + token);
				}

				token = st.nextToken();
				if (!token.equalsIgnoreCase("="))
				{
					System.out.println("Expecting '=' and got: " + token);
				}

				// Now we should be at the name of file being linked to.
				hyperlink = st.nextToken();
				if (!hyperlink.startsWith("page"))
				{
					System.out.println("Expecting 'page#.html' and got: " + hyperlink);
				}

				token = st.nextToken();
				if (!token.equalsIgnoreCase(">"))
				{
					System.out.println("Expecting '>' and got: " + token);
				}

				if (DEBUGGING) System.out.println(" - found a link to " + hyperlink);

				//////////////////////////////////////////////////////////////////////
				// Have collected a child node; now have to decide what to do with it.
				//////////////////////////////////////////////////////////////////////

				if (alreadyInOpen(hyperlink))
				{ // If already in OPEN, we'll ignore this hyperlink
					// (Be sure to read the "Technical Note" below.)
					
					if (DEBUGGING) System.out.println(" - this node is in the OPEN list.");
				}
				else if (CLOSED.contains(hyperlink))
				{ // If already in CLOSED, we'll also ignore this hyperlink.
					if (DEBUGGING) System.out.println(" - this node is in the CLOSED list.");
				}
				else 
				{ 
					String hypertext = ""; // The text associated with this hyperlink.

					do
					{
						token = st.nextToken();
						if (!token.equalsIgnoreCase("</A>")) hypertext += " " + token;
					}
					while (!token.equalsIgnoreCase("</A>"));

					if (DEBUGGING) System.out.println("   with hypertext: " + hypertext);
					
					//For BEST first, we need to calculate the H2 & H3 Value for the hypertext.
					//Once we have these values, we add them to the HashMap entries.
					if(searchStrategy.compareToIgnoreCase("best") != 0){
						//For DFS and BFS,simply add the new node to the priority Q.
						OPEN.add(new SearchNode(hyperlink));
						visitedLinks.add(hyperlink);
						SearchNode.putEntryInMap(hyperlink, parent.nodeName);
					}else{
						
						//Add the hyperlink to the list of children.
						//Once we are finsihed with updating, we will increase the priority of every Hyperlink on this
						//page by the value of h1 hueristic of this page.
						if(!children.contains(hyperlink)){
							children.add(hyperlink);
						}
						
						//The more no. of QUERY words in hypertext, the more the H2 value of the hyperlink
						//The more consecutive & in numerical order the QUERY words, the more the H3 val of Hyperlink.
						if ((qOccurance = returnH2AndH3Sum(hypertext)) > 0){
							//Populate the priority in the HashMap for now. The actual Priority Queue will be update at the end
							if(childPriorities.containsKey(hyperlink)){
								double pr = (double)childPriorities.get(hyperlink);
								childPriorities.put(hyperlink, pr+(double)qOccurance);
							}else{
								childPriorities.put(hyperlink,(double)qOccurance);
							}
						}
					}

					// Technical note: in best-first search,
					// if a page contains TWO (or more) links to the SAME page,
					// it is acceptable if only the FIRST one is inserted into OPEN,
					// rather than the better-scoring one.  For simplicity, once a node
					// has been placed in OPEN or CLOSED, we won't worry about the
					// possibility of later finding of higher score for it.
					// Since we are scoring the hypertext POINTING to a page,
					// rather than the web page itself, we are likely to get
					// different scores for given web page.  Ideally, we'd
					// take this into account when sorting OPEN, but you are
					// NOT required to do so (though you certainly are welcome
					// to handle this issue).

					// HINT: read about the insertElementAt() and addElement()
					// methods in the Vector class.
				}
			}
		}
		
		if(searchStrategy.compareToIgnoreCase("best") == 0){
			//Now that the entire page has been parsed, its time to update the H1 value for all the children hyper-links
			
			for(String link : children){
				if(childPriorities.containsKey(link)){
					double pr = (double)childPriorities.get(link);
					childPriorities.put(link, pr+(double)h1Val);
				}else{
					childPriorities.put(link,(double)h1Val);
				}
			}
			
			
			//Iterate over the temp hashMap containing all priorities and update the global PriorityQue
			Iterator childPrItr = childPriorities.entrySet().iterator();
			while(childPrItr.hasNext()){
				Map.Entry pair = (Map.Entry)childPrItr.next();
				
				String link = pair.getKey().toString();
				//If the global Priority Q already contains the H val for this link, ignore.
				//If not, then update the global Priority Q
				if(!alreadyInOpen(link) && !CLOSED.contains(link)){
					double pr = (double)pair.getValue();
					SearchNode node = new SearchNode(link, (long)pr);
					OPEN.add(node);
					visitedLinks.add(link);
				}	
				
				SearchNode.putEntryInMap(link, parent.nodeName);
			}
		}
		
	}

	// A GOAL is a page that contains the goalPattern set above.
	static boolean isaGoalNode(String contents)
	{
		return (contents != null && contents.indexOf(GOAL_PATTERN) >= 0);
	}

	// Is this hyperlink already in the OPEN list?
	// This isn't a very efficient way to do a lookup,
	// but its fast enough for this homework.
	// Also, this for-loop structure can be
	// be adapted for use when inserting nodes into OPEN
	// according to their heuristic score.
	static boolean alreadyInOpen(String hyperlink)
	{
		if(visitedLinks.contains(hyperlink))
			return true;

		return false;  // Not in OPEN.    
	}

	// You can use this to remove the first element from OPEN.
	static SearchNode pop()
	{
		SearchNode result = OPEN.poll();
		String linkName = result.nodeName;
		//System.out.println(linkName + " : " + result.priority);
		visitedLinks.remove(linkName);
		return result;
	}
	
	static int returnH2AndH3Sum(String hypertext){
		return (calculateH2Val(hypertext)+calculateH3Val(hypertext));
	}
	
	static int calculateH2Val(String str){
		int count = 0;
		StringTokenizer st = new StringTokenizer(str);
		
		while (st.hasMoreTokens()){
			if(st.nextToken().contains("QUERY")){
				count++;
			}
		}
		
		return count;
	}
	
	static int calculateH3Val(String str){
		
		int hVal = 0;
		StringTokenizer st = new StringTokenizer(str);
		String prevToken = "";
		
		while (st.hasMoreTokens())
		{
			String token = st.nextToken();
			
			//Checking for consecutive and numerical order
			if(token.contains("QUERY")){
				if(prevToken.contains("QUERY")){
					System.out.println(prevToken + " : " + token);
					if(token.compareToIgnoreCase(prevToken) > 0){
						hVal += 1;
					}
				}
			}
			
			prevToken = token;
		}
		
		return hVal;
	}
}

/////////////////////////////////////////////////////////////////////////////////

//You'll need to design a Search node data structure.

//Note that the above code assumes there is a method called getHvalue()
//that returns (as a double) the heuristic value associated with a search node,
//a method called getNodeName() that returns (as a String)
//the name of the file (eg, "page7.html") associated with this node, and
//a (void) method called reportSolutionPath() that prints the path
//from the start node to the current node represented by the SearchNode instance.
class SearchNode
{
	final String nodeName;
	long priority; 
	private static HashMap<String,String> childParentMap = new HashMap<String,String>();
	
	public SearchNode(String name) {
		nodeName = name;
		//priority = System.currentTimeMillis();
		priority = System.nanoTime();
	}
	
	public SearchNode(String name, long pri) {
		nodeName = name;
		priority = pri;
	}
	
	public int reportSolutionPath() {
		int pathLen = -1;
		System.out.println("Solution Path BACKWARDS");
		System.out.print(this.nodeName);
		pathLen++;
		
		String currentNodeName = this.nodeName;
		while(childParentMap.containsKey(currentNodeName)){
			System.out.print("<-" + childParentMap.get(currentNodeName).toString());
			currentNodeName = new String(childParentMap.get(currentNodeName).toString());
			pathLen++;
		}
		System.out.println("\n");
		
		return pathLen;
	}
	
	public static void putEntryInMap(String child, String parent){
		if(!childParentMap.containsKey(child)){
			childParentMap.put(child, parent);
		}
	}
	
	public String getNodeName() {
		return nodeName;
	}

}


class DfsQComparator implements Comparator<SearchNode>
{
	 public int compare(SearchNode node1, SearchNode node2)
     {
         if(node1.priority < node2.priority){
        	 return 1;
         }
         if(node1.priority > node2.priority){
        	 return -1;
         }
         return 0;
     }
	
}

class BfsQComparator implements Comparator<SearchNode>
{
	 public int compare(SearchNode node1, SearchNode node2)
     {
         if(node1.priority > node2.priority){
        	 return 1;
         }
         if(node1.priority < node2.priority){
        	 return -1;
         }
         return 0;
     }
	
}

class BestQComparator implements Comparator<SearchNode>
{
	 public int compare(SearchNode node1, SearchNode node2)
     {
         if(node1.priority < node2.priority){
        	 return 1;
         }
         if(node1.priority > node2.priority){
        	 return -1;
         }
         return 0;
     }
	
}




