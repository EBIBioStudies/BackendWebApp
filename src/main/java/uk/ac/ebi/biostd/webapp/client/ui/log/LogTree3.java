package uk.ac.ebi.biostd.webapp.client.ui.log;

import java.util.List;

import uk.ac.ebi.biostd.treelog.LogNode;

import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.TreeNode;
import com.smartgwt.client.widgets.tree.events.FolderOpenedEvent;
import com.smartgwt.client.widgets.tree.events.FolderOpenedHandler;

public class LogTree3 extends TreeGrid
{
 Tree data = new Tree();
 
 public LogTree3( final LogNode root )
 {
  setWidth100();
  setHeight100();
  
  setShowConnectors(true);
  setShowHeader(false);
  
  setScrollRedrawDelay(1);
//  Tree data = new Tree();
  
  if( root == null )
  {
   data.setRoot(new TreeNode());
   
   return;
  }
  
  
  TreeNode rootNode = new TreeNode();
  

  final LogTreeNode  clsRoot = new LogTreeNode(root);
  clsRoot.setTitle("Log");
  clsRoot.setIsFolder(true);
  
  data.setRoot(rootNode);
  data.addList(new TreeNode[] { clsRoot } , rootNode);

  fillNode(clsRoot);

  //  createTreeStructure(root, clsRoot);
  
  setData(data);
  
  addFolderOpenedHandler( new FolderOpenedHandler()
  {
   @Override
   public void onFolderOpened(FolderOpenedEvent event)
   {
    
    TreeNode tnd = event.getNode();
    if( ! tnd.getAttributeAsBoolean("_expanded") )
    {
     fillNode( (LogTreeNode)tnd );
    }
    
   }
  });
  
//  data.openFolder(clsRoot);
  
//  expandRecord(clsRoot);

//  data.openAll();

 }
 
 private void fillNode( LogTreeNode nd )
 {
  LogNode n  = nd.getNode();
  
//  SC.warn("LogNode: "+n.getMessage()+" - "+(n.getSubNodes()==null?"null":n.getSubNodes().size()));

  
  if( n.getSubNodes() == null )
   return;
  
  TreeNode[] nodes = new TreeNode[ n.getSubNodes().size() ];
  
  int i=0;
  
  for( LogNode ln : n.getSubNodes() )
  {
   nodes[i++] = new LogTreeNode(ln);
  }
  
  data.addList( nodes , nd);
  
  nd.setAttribute("_expanded", true);
 }
 
 @Override
 protected void onDraw()
 {
  super.onDraw();
  
  TreeNode rn = data.getRoot();
  TreeNode[] rCh = data.getChildren(rn);
  
  if( rCh.length > 0 )
   data.openFolder( rCh[0] );
 }
 
 private void createTreeStructure(LogNode cls, LogTreeNode node)
 {
  
  if( cls.getSubNodes() == null)
   return;

  List<? extends LogNode> subNodes = cls.getSubNodes();
  
  TreeNode[] children = new TreeNode[ subNodes.size() ];


  for( int i=0; i < subNodes.size(); i++ )
  {
   LogNode subLn = subNodes.get(i);
   
   LogTreeNode snd = new LogTreeNode( subLn );
   children[i] = snd;

   createTreeStructure(subLn, snd);
  }
  
  data.addList(children, node);
 }
}
