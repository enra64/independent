package de.oerntec.votenote;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SeekBar.OnSeekBarChangeListener;

@SuppressLint("InflateParams")
public class GroupManagementActivity extends Activity {
	
	static DBGroups groupsDB;
	static DBEntries entriesDB;
	
	ListView mainList;
	
	static SimpleCursorAdapter groupAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_group_management);
		
		//get db
		groupsDB=new DBGroups(this);
		entriesDB=new DBEntries(this);
		
		//config listview
		// set up the drawer's list view with items and click listener
		Cursor allCursor=groupsDB.allGroupNamesAndMinvotes();
		
		//define wanted columns
		String[] columns = {DatabaseCreator.GROUPS_NAMEN, DatabaseCreator.GROUPS_MIN_VOTE};
		
		//define id values of views to be set
		int[] to= {R.id.textGroupName, R.id.textMinVote};
		
		// create the adapter using the cursor pointing to the desired data 
		//as well as the layout information
		groupAdapter = new SimpleCursorAdapter(
		    this, 
		    R.layout.listitem_managegroups, 
		    allCursor, 
		    columns, 
		    to,
		    0);
		
		//get listview
		mainList=(ListView) findViewById(R.id.listGroupManagement);
		mainList.setAdapter(groupAdapter);
		mainList.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, final long id) {
				Log.i("gmanage", "handling listview click on "+view.toString());
				showNameActionsDialog(position);
				
			}
		});
	}

	private void showNameActionsDialog(final int position) {
		//get name of the group supposed to be deleted
		Cursor allCursor=groupsDB.allGroupNames();
		allCursor.moveToPosition(position);
		final String oldName=allCursor.getString(1);
		allCursor.close();
		
		new AlertDialog.Builder(this)
	    .setTitle(oldName)
	    .setPositiveButton("Ändern", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	        	showChangeDialog(position);
	        }
	    }).setNegativeButton("Löschen", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	        	showDeleteDialog(position);
	        }
	    }).show();
	}
	
	protected void showChangeDialog(final int pos) {
		final int changePosition=groupsDB.translateDrawerSelectionToDBID(pos);
		//get old name
		Cursor allCursor=groupsDB.allGroupNames();
		allCursor.moveToPosition(pos);
		final String oldName=allCursor.getString(1);
		allCursor.close();
		
		//inflate view with seekbar and name
		final View input=this.getLayoutInflater().inflate(R.layout.dialog_newgroup, null);
		final EditText nameInput=(EditText) input.findViewById(R.id.editNewName);
		final TextView infoView=(TextView) input.findViewById(R.id.minVotInfoText);
		
		//offer hint to user
		nameInput.setText(oldName);
		
		//initialize seekbar and seekbar info text
		int oldMinVote=groupsDB.getMinVote(changePosition);
		infoView.setText(oldMinVote+"%");
		
		final SeekBar minVoteSeek=(SeekBar) input.findViewById(R.id.newMinVotSeek);
		minVoteSeek.setMax(100);
		minVoteSeek.setProgress(oldMinVote);
		minVoteSeek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {       
			@Override public void onStopTrackingTouch(SeekBar seekBar) {}       
			@Override public void onStartTrackingTouch(SeekBar seekBar){}
			@Override
			public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
				infoView.setText(progress+"%");
			}
		});
		
		//build alertdialog
		Builder b=new AlertDialog.Builder(this)
	    .setView(input)
	    .setTitle(oldName+" ändern")
	    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	        	//change minimum vote value
	        	int minVotValue=minVoteSeek.getProgress();
	            groupsDB.changeMinVote(changePosition, minVotValue);
	            
	            //change group name if it changed
	            String newName = nameInput.getText().toString();
	            if(!newName.equals(oldName)&&newName!=""){
	            	groupsDB.changePositionTO(changePosition, newName);
	            }
	            groupAdapter.swapCursor(groupsDB.allGroupNamesAndMinvotes()).close();
	        }
	    }).setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int whichButton) {}
	    });
		b.create().show();
	}

	protected void showDeleteDialog(final int deletePosition) {
		//get name of the group supposed to be deleted
		Cursor allCursor=groupsDB.allGroupNames();
		allCursor.moveToPosition(deletePosition);
		String groupName=allCursor.getString(1);
		final int confirmedGroupID=allCursor.getInt(0);
		allCursor.close();
		
		//request confirmation
		new AlertDialog.Builder(this)
	    .setTitle(groupName+" löschen?")
	    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	        	groupsDB.deleteGroupAtPos(deletePosition);
	        	entriesDB.deleteAllEntriesForGroup(confirmedGroupID);
	        	groupAdapter.swapCursor(groupsDB.allGroupNamesAndMinvotes()).close();
	        }
	    }).setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
	    	public void onClick(DialogInterface dialog, int whichButton) {}
	    }).show();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_add_group) {
			createGroupDialog(this, false);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.group_management, menu);
		return true;
	}
	
	/**
	 * Creates the Dialog responsible for asking the user the name
	 * @param context Context to use
	 * @param firstGroup Whether we are creating the users first group
	 */
	private static void createGroupDialog(final Context context, boolean firstGroup){
		//inflat view with seekbar and name
		final View input=((Activity) context).getLayoutInflater().inflate(R.layout.dialog_newgroup, null);
		final EditText nameInput=(EditText) input.findViewById(R.id.editNewName);
		nameInput.setHint("Übungsname");
		
		final TextView infoView=(TextView) input.findViewById(R.id.minVotInfoText);
		
		final SeekBar minVoteSeek=(SeekBar) input.findViewById(R.id.newMinVotSeek);
		minVoteSeek.setMax(100);
		minVoteSeek.setProgress(50);
		minVoteSeek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {       
			@Override public void onStopTrackingTouch(SeekBar seekBar) {}       
			@Override public void onStartTrackingTouch(SeekBar seekBar){}
			@Override
			public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
				infoView.setText(progress+"%");
			}
		});
		
		//build alertdialog
		Builder b=new AlertDialog.Builder(context)
	    .setView(input)
	    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	            String name = nameInput.getText().toString();
	            int minVotValue=minVoteSeek.getProgress();
	            if(groupsDB.changeOrAddGroupName(name, minVotValue)==-1)
	            	Toast.makeText(context, "Übung existiert schon", Toast.LENGTH_SHORT).show();
	            else
	            	groupAdapter.swapCursor(groupsDB.allGroupNamesAndMinvotes()).close();
	        }
	    }).setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int whichButton) {}
	    });
		if(firstGroup){
			b.setTitle("Erste Übung erstellen")
			.setMessage("Bitte Übung anwählen");
		}
		else
			b.setTitle("Übung erstellen");
		b.create().show();
	}
}
