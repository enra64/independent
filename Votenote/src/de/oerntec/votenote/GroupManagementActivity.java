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
		
		/*
		 * handle if first group: show dialog
		 * get savedinstance state, 
		 */
		if(getIntent().getExtras().getBoolean("firstGroup", false)){
			Builder b=new AlertDialog.Builder(this);
		    b.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int whichButton) {}
		    }).setTitle("Erste �bung")
		    .setMessage("Tippe auf das Plus um die �bung zu erstellen").create().show();
		}
		//get db
		groupsDB=new DBGroups(this);
		entriesDB=new DBEntries(this);
		
		/*
		 * create listview
		 */
		// set up the drawer's list view with items and click listener
		Cursor allCursor=groupsDB.allGroupsAllInfo();
		//define wanted columns
		String[] columns = {DatabaseCreator.GROUPS_NAMEN, DatabaseCreator.GROUPS_MIN_VOTE, DatabaseCreator.GROUPS_MIN_PRESENTATIONPOINTS};
		
		//define id values of views to be set
		int[] to= {R.id.listitem_groupmanager_groupname, R.id.listitem_groupmanager_minvote, R.id.listitem_groupmanager_minprespoints};
		
		// create the adapter using the cursor pointing to the desired data
		groupAdapter = new SimpleCursorAdapter(
		    this, 
		    R.layout.listitem_managegroups_new, 
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
	    .setPositiveButton("�ndern", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	        	showChangeDialog(position);
	        }
	    }).setNegativeButton("L�schen", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	        	showDeleteDialog(position);
	        }
	    }).show();
	}
	
	protected void showChangeDialog(final int pos) {
		final int databaseID=groupsDB.translateDrawerSelectionToDBID(pos);
		//get old name
		Cursor allCursor=groupsDB.allGroupNames();
		allCursor.moveToPosition(pos);
		final String oldName=allCursor.getString(1);
		allCursor.close();
		
		//inflate view with seekbar and name
		final View input=this.getLayoutInflater().inflate(R.layout.dialog_newgroup, null);
		final EditText nameInput=(EditText) input.findViewById(R.id.editNewName);
		final TextView voteInfo=(TextView) input.findViewById(R.id.minVotInfoText);
		final SeekBar minVoteSeek=(SeekBar) input.findViewById(R.id.newMinVotSeek);
		final TextView presInfo=(TextView) input.findViewById(R.id.dialogNewGroupMinPresInfoText);
		final SeekBar minPresSeek=(SeekBar) input.findViewById(R.id.dialogNewGroupPresSeek);
		
		//minpresseek
		int prevMinPresPoints=groupsDB.getMinPresPoints(databaseID);
		presInfo.setText(""+prevMinPresPoints);
		minPresSeek.setProgress(prevMinPresPoints);
		minPresSeek.setMax(5);
		minPresSeek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {       
			@Override public void onStopTrackingTouch(SeekBar seekBar) {}       
			@Override public void onStartTrackingTouch(SeekBar seekBar){}
			@Override
			public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
				presInfo.setText(String.valueOf(progress));
			}
		});
		
		//minvoteseek
		//offer hint to user
		nameInput.setText(oldName);
		
		//initialize seekbar and seekbar info text
		int oldMinVote=groupsDB.getMinVote(databaseID);
		voteInfo.setText(oldMinVote+"%");
		
		
		minVoteSeek.setMax(100);
		minVoteSeek.setProgress(oldMinVote);
		minVoteSeek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {       
			@Override public void onStopTrackingTouch(SeekBar seekBar) {}       
			@Override public void onStartTrackingTouch(SeekBar seekBar){}
			@Override
			public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
				voteInfo.setText(progress+"%");
			}
		});
		
		//build alertdialog
		Builder b=new AlertDialog.Builder(this)
	    .setView(input)
	    .setTitle(oldName+" �ndern")
	    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	        	//change minimum vote, pres value
	            groupsDB.setMinVote(databaseID, minVoteSeek.getProgress());
	            if(groupsDB.setMinPresPoints(databaseID, minPresSeek.getProgress())!=1)
	            	Log.e("groupmanager", "did not update one row");
	            //change group name if it changed
	            String newName = nameInput.getText().toString();
	            if(!newName.equals(oldName)&&newName!="")
	            	groupsDB.changePositionTO(databaseID, newName);
	            groupAdapter.swapCursor(groupsDB.allGroupsAllInfo()).close();
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
	    .setTitle(groupName+" l�schen?")
	    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	        	groupsDB.deleteGroupAtPos(deletePosition);
	        	entriesDB.deleteAllEntriesForGroup(confirmedGroupID);
	        	groupAdapter.swapCursor(groupsDB.allGroupsAllInfo()).close();
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
		final TextView presInfo=(TextView) input.findViewById(R.id.dialogNewGroupMinPresInfoText);
		final SeekBar minPresSeek=(SeekBar) input.findViewById(R.id.dialogNewGroupPresSeek);
		final TextView infoView=(TextView) input.findViewById(R.id.minVotInfoText);
		final SeekBar minVoteSeek=(SeekBar) input.findViewById(R.id.newMinVotSeek);
		nameInput.setHint("�bungsname");
		
		//minpresseek
		presInfo.setText("2");
		
		minPresSeek.setMax(5);
		minPresSeek.setProgress(2);
		minPresSeek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {       
			@Override public void onStopTrackingTouch(SeekBar seekBar) {}       
			@Override public void onStartTrackingTouch(SeekBar seekBar){}
			@Override
			public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
				presInfo.setText(String.valueOf(progress));
			}
		});
		
		//minvoteseek
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
	            if(groupsDB.addGroup(name, minVotValue, minPresSeek.getProgress())==-1)
	            	Toast.makeText(context, "�bung existiert schon", Toast.LENGTH_SHORT).show();
	            else
	            	groupAdapter.swapCursor(groupsDB.allGroupsAllInfo()).close();
	        }
	    }).setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int whichButton) {}
	    });
		if(firstGroup){
			b.setTitle("Erste �bung erstellen")
			.setMessage("Bitte �bung anw�hlen");
		}
		else
			b.setTitle("�bung erstellen");
		b.create().show();
	}
}
