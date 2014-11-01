package de.oerntec.votenote;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.NumberPicker.OnValueChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.NumberPicker.OnScrollListener;

@SuppressLint("InflateParams")
public class MainActivity extends Activity implements NavigationDrawerFragment.NavigationDrawerCallbacks {

	/**
	 * Fragment managing the behaviors, interactions and presentation of the
	 * navigation drawer.
	 */
	private static NavigationDrawerFragment mNavigationDrawerFragment;

	
	/**
	 * Used to store the last screen title. For use in
	 * {@link #restoreActionBar()}.
	 */
	private CharSequence mTitle;
	
	private static DBGroups groupDB;
	private static DBEntries entryDB;
	
	private static int mCurrentSelectedPosition;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//database access
		groupDB=new DBGroups(this);
		entryDB=new DBEntries(this);
		
		mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
		mTitle = getTitle();

		// Set up the drawer.
		mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));
		
	}

	/**
	 * position is 0 indexed; the phf gets position+1
	 */
	@Override
	public void onNavigationDrawerItemSelected(int position) {
		//keep track of what fragment is shown
		mCurrentSelectedPosition=position;
		// update the main content by replacing fragments
		Log.i("votenote main", "selected fragment "+position);
		FragmentManager fragmentManager = getFragmentManager();
		fragmentManager
				.beginTransaction()
				.replace(R.id.container,
						PlaceholderFragment.newInstance(position)).commit();
	}

	/**
	 * Gets the current section name from the database, and adds it to the view
	 * @param section datbase index+1
	 */
	public void onSectionAttached(int section) {
		Cursor allNames=groupDB.allGroupNames();
		if(allNames.getCount()!=0){
			allNames.moveToPosition(section);
			mTitle=allNames.getString(1);
		}
		else
			mTitle="Übung hinzufügen!";
	}

	public void restoreActionBar() {
		ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle(mTitle);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!mNavigationDrawerFragment.isDrawerOpen()) {
			// Only show items in the action bar relevant to this screen
			// if the drawer is not showing. Otherwise, let the drawer
			// decide what to show in the action bar.
			getMenuInflater().inflate(R.menu.main, menu);
			restoreActionBar();
			return true;
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final int groupID=groupDB.translateDrawerSelectionToDBID(mCurrentSelectedPosition);
		
		switch(item.getItemId()){
			case R.id.action_groupmanagement:
				Intent intent = new Intent(this, GroupManagementActivity.class);
				startActivity(intent);
				break;
			case R.id.action_add_group:
				createGroupDialog(this, false);
				break;
			case R.id.action_add_entry:
				createEntryDialog(groupID, true, 0);
				break;
			case R.id.action_prespoints:
				createPresPointsDialog(groupID);
				break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void createPresPointsDialog(final int dataBaseId) {
		final View inputView=this.getLayoutInflater().inflate(R.layout.dialog_changeprespoints, null);
		final TextView presPointsView=(TextView) inputView.findViewById(R.id.prespointView);
		final Button plusButton=(Button) inputView.findViewById(R.id.increasePresPoints);
		final Button minusButton=(Button) inputView.findViewById(R.id.decreasePresPoints);
		presPointsView.setText(String.valueOf(groupDB.getPresPoints(dataBaseId)));
		plusButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				int prevValue=Integer.getInteger(presPointsView.getText().toString());
				presPointsView.setText(prevValue+1);
			}
		});
		minusButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				int prevValue=Integer.getInteger(presPointsView.getText().toString());
				presPointsView.setText(prevValue-1);
			}
		});
		//build alertdialog
		Builder b=new AlertDialog.Builder(this)
	    .setView(inputView)
	    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	            int presPoints=Integer.getInteger(presPointsView.getText().toString());
	            groupDB.setPresPoints(dataBaseId, presPoints);
	            mNavigationDrawerFragment.forceReload();
	        }
	    }).setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int whichButton) {}
	    });
		b.setTitle("Erreichte Praesentationspunkte");
		b.create().show();
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
	            if(groupDB.changeOrAddGroupName(name, minVotValue)==-1)
	            	Toast.makeText(context, "Übung existiert schon", Toast.LENGTH_SHORT).show();
	            else
	            	mNavigationDrawerFragment.forceReload();
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
	
	/**
	 * Dialog for creating an entry
	 * @param groupID
	 * @param add
	 * @param uebungNummer
	 */
	private void createEntryDialog(final int groupID, boolean add, int uebungNummer){
		//if we add this entry, we use these values;
		int maxVoteValue=0, minVoteValue=0;
		if(add){
			maxVoteValue=entryDB.getPrevMaxVote(groupID);
			minVoteValue=3;
		}
		//else we have to get them from the database
		else{
			Cursor oldValues=entryDB.getEntry(groupID, uebungNummer);
			maxVoteValue=oldValues.getInt(1);
			minVoteValue=oldValues.getInt(0);
		}
		//inflate view, find the textview containing the explanation
		final View pickView=this.getLayoutInflater().inflate(R.layout.dialog_new_entry, null);
		final TextView infoView=(TextView) pickView.findViewById(R.id.infoTextView);
		
		//find and configure the number pickers
        final NumberPicker maxVote=(NumberPicker) pickView.findViewById(R.id.pickerMaxVote);
        maxVote.setMinValue(0);
        maxVote.setMaxValue(15);
        maxVote.setValue(maxVoteValue);
        final NumberPicker myVote=(NumberPicker) pickView.findViewById(R.id.pickerMyVote);
        myVote.setMinValue(0);
        myVote.setMaxValue(15);
        myVote.setValue(minVoteValue);
        
        //set the current values of the picers as explanation text
        infoView.setText(myVote.getValue()+" von "+maxVote.getValue()+" Votes");
        infoView.setTextColor(Color.argb(255, 153, 204, 0));//green
        
       //add change listener to update dialog expl. if pickers changed
        myVote.setOnValueChangedListener (new OnValueChangeListener() {
        @Override
		public void onValueChange(NumberPicker thisPicker, int arg1, int newVal) {
			int myVoteValue=newVal;
    		int maxVoteValue=maxVote.getValue();
			infoView.setText(myVoteValue+" von "+maxVoteValue+" Votes");
			if(maxVoteValue>=myVoteValue)
				infoView.setTextColor(Color.argb(255, 153, 204, 0));//green
			else
				infoView.setTextColor(Color.argb(255, 204, 0, 0));//red
		}
        });
        maxVote.setOnValueChangedListener(new OnValueChangeListener() {@Override
			public void onValueChange(NumberPicker thisPicker, int arg1, int newVal) {
        	int myVoteValue=myVote.getValue();
    		int maxVoteValue=newVal;
			infoView.setText(myVoteValue+" von "+maxVoteValue+" Votes");
			if(maxVoteValue>=myVoteValue)
				infoView.setTextColor(Color.argb(255, 153, 204, 0));//green
			else
				infoView.setTextColor(Color.argb(255, 204, 0, 0));//red
			}
        });
        
        //build alertdialog
		new AlertDialog.Builder(this)
	    .setTitle("Neuer Eintrag")
	    .setView(pickView)
	    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	        	if(myVote.getValue()<=maxVote.getValue()){
	        		//reload current fragment
		        	onNavigationDrawerItemSelected(mCurrentSelectedPosition);
		        	Log.i("votenote:addentry", "reloading fragment "+mCurrentSelectedPosition);
		            entryDB.addEntry(groupID, maxVote.getValue(), myVote.getValue());
	        	}
	        	else
	        		Toast.makeText(getApplicationContext(), "Mehr votiert als möglich!", Toast.LENGTH_SHORT).show();
	        }
	    }).setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {}
	    }).show();
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		private static final String ARG_SECTION_NUMBER = "section_number";
		
		/**
		 * retains the fragment id b/c placerholderfragment somehow fucks up
		 */
		private static int mSectionNumber;

		/**
		 * Returns a new instance of this fragment for the given section number.
		 */
		public static PlaceholderFragment newInstance(int sectionNumber) {
			Log.i("votenote placeholder", "newinstance");
			PlaceholderFragment fragment = new PlaceholderFragment();
			Bundle args = new Bundle();
			mSectionNumber=sectionNumber;
			args.putInt(ARG_SECTION_NUMBER, sectionNumber);
			fragment.setArguments(args);
			return fragment;
		}
		
		/*
		 * fragment_main gets built
		 */
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			//db access
			final DBEntries entryDB=new DBEntries(getActivity());
			final DBGroups groupDB=new DBGroups(getActivity());
			
			//translate from position in drawer to db group id
			final int translatedSection= groupDB.translateDrawerSelectionToDBID(mSectionNumber);

			//find and inflate everything
			View rootView = inflater.inflate(R.layout.fragment_main, container, false);
			ListView voteList=(ListView) rootView.findViewById(R.id.voteList);
			final TextView summaryView=(TextView) rootView.findViewById(R.id.voteAverage);

			//if translatedSection is -1, no group has been added yet
			if(translatedSection!=DBGroups.NO_GROUPS_EXIST){
				Log.i("votenote main", "displaying entries for group "+translatedSection+" (according to group db)");
				
				Cursor allEntryCursor=entryDB.getGroupRecords(translatedSection);
				
				//create listview adapter
				//define wanted columns
				String[] columns = {DatabaseCreator.ENTRIES_NUMMER_UEBUNG, DatabaseCreator.ENTRIES_MY_VOTES, DatabaseCreator.ENTRIES_MAX_VOTES};
				
				//define id values of views to be set
				int[] toViews={R.id.textUebungNummer, R.id.textMyVote, R.id.textMaxVotes};
				
				// create the adapter using the cursor pointing to the desired data 
				//as well as the layout information
				final SimpleCursorAdapter groupAdapter = new SimpleCursorAdapter(
				    getActivity(), 
				    R.layout.listitem_main, 
				    allEntryCursor, 
				    columns, 
				    toViews,
				    0);
				voteList.setAdapter(groupAdapter);
				//votelist adaptering finished
				
				//creating onclicklistener for showing change dialog
				voteList.setOnItemClickListener(new OnItemClickListener(){
					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, final long id) {
						//beginning to configure dialog view
						final int translatedPosition=position+1;
						
						//inflate view, find the textview containing the explanation
						final View pickView=getActivity().getLayoutInflater().inflate(R.layout.dialog_new_entry, null);
						final TextView infoView=(TextView) pickView.findViewById(R.id.infoTextView);
						
						//find number pickers
				        final NumberPicker maxVote=(NumberPicker) pickView.findViewById(R.id.pickerMaxVote);
				        final NumberPicker myVote=(NumberPicker) pickView.findViewById(R.id.pickerMyVote);
				        //configure the number pickers for translatedposition, connect to infoview
				        configureNumberPickers(infoView, maxVote, myVote, translatedPosition);
				        //finished configuring view
				     
				        //building alertdialog with the view just configured
						new AlertDialog.Builder(getActivity())
					    .setTitle("Eintrag ändern")
					    .setView(pickView)
					    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					        public void onClick(DialogInterface dialog, int whichButton) {
					        	if(myVote.getValue()<=maxVote.getValue()){//check for valid entry
					        		//change db entry
					        		entryDB.changeEntry(translatedSection, translatedPosition, maxVote.getValue(), myVote.getValue());
									//reload list- and textview; close old cursor
					        		groupAdapter.swapCursor(entryDB.getGroupRecords(translatedSection)).close();
					        		setVoteAverage(translatedSection, summaryView);
					        	}
					        	else
					        		Toast.makeText(getActivity(), "Mehr votiert als möglich!", Toast.LENGTH_SHORT).show();
					        }
					    }).setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
					        public void onClick(DialogInterface dialog, int whichButton) {}
					    }).show();
					}

					private void configureNumberPickers(final TextView infoView, final NumberPicker maxVote, final NumberPicker myVote, int translatedPosition) {
						//get the old values from the database; position +1 because natural counting style
						//is used when counting the uebungs
						Cursor oldValues=entryDB.getEntry(translatedSection, translatedPosition);
						maxVote.setMinValue(1);
				        maxVote.setMaxValue(15);
				        maxVote.setValue(oldValues.getInt(1));
				        myVote.setMinValue(0);
				        myVote.setMaxValue(15);
				        myVote.setValue(oldValues.getInt(0));
				      //add change listener to update dialog if pickers changed
				        myVote.setOnScrollListener(new OnScrollListener() {@Override
							public void onScrollStateChange(NumberPicker thisPicker, int isIdle) {
								infoView.setText(thisPicker.getValue()+" von "+maxVote.getValue()+" Votes");
							}
				        });
				        maxVote.setOnScrollListener(new OnScrollListener() {@Override
							public void onScrollStateChange(NumberPicker thisPicker, int isIdle) {
								infoView.setText(myVote.getValue()+" von "+thisPicker.getValue()+" Votes");
							}
				        });
				      //set the current values of the pickers as explanation text
				        infoView.setText(myVote.getValue()+" von "+maxVote.getValue()+" Votes");
					}
				});
				
				//set summaryView to average
				setVoteAverage(translatedSection, summaryView);
			}
			else{
				createGroupDialog(getActivity(), true);
				//reload drawer...
				summaryView.setText("Füge einen Eintrag hinzu");
			}
			return rootView;
		}
		
		private void setVoteAverage(int forSection, TextView affectView){
			//logging b/c i am slightly retarded
			Log.i("votenote:calcavg", "calculating average for section "+forSection);
			//get avg cursor
			Cursor avgCursor=entryDB.getGroupRecords(forSection);
			int maxVoteCount=0, myVoteCount=0;
			for(int i=0; i<avgCursor.getCount();i++){
				myVoteCount+=avgCursor.getInt(1);
				maxVoteCount+=avgCursor.getInt(2);
				avgCursor.moveToNext();
			}
			//close the cursor; it did its job
			avgCursor.close();
			
			//no votes have been given
			if(maxVoteCount==0){
				//Log.w("votenote:calcavg", "maxvote is 0; aborting");
				affectView.setText("Füge einen Eintrag ein.");
			}
			//calc percentage
			float average=((float)myVoteCount/maxVoteCount)*100;
			int avg=(int)average;
			
			//get minvote for section
			int minVote=groupDB.getMinVote(forSection);
			
			//write percentage and color coding to summaryview
			affectView.setText(avg+"%");
			
			if(avg>=minVote)
				affectView.setTextColor(Color.argb(255, 153, 204, 0));//green
			else
				affectView.setTextColor(Color.argb(255, 204, 0, 0));//red
		}
		
		@Override
		public void onAttach(Activity activity) {
			super.onAttach(activity);
			((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
		}
	}

	
}
/* retired code for changing minimum vote count
//change minval
if (id == R.id.action_change_minvalue) {
	//inflate view, find necessary views
	final View minValView=this.getLayoutInflater().inflate(R.layout.dialog_change_minvalue, null);
	final TextView minText=(TextView) minValView.findViewById(R.id.changeMinValText);
	final SeekBar minSeek=(SeekBar) minValView.findViewById(R.id.changeMinValSeek);
	
	//get old min values
	int oldMinVote=groupDB.getMinVote(translatedPosition);
	//no groups exist, abort
	if(oldMinVote==DBGroups.NO_GROUPS_EXIST){
		Toast.makeText(this, "Erstelle zuerst eine Gruppe!", Toast.LENGTH_SHORT);
		return true;
	}
	//set old minimum vote as start values
	minSeek.setProgress(oldMinVote);
	minText.setText(oldMinVote+"%");
	
	//add listener to constantly change textview
	minSeek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {       
		@Override public void onStopTrackingTouch(SeekBar seekBar) {}       
		@Override public void onStartTrackingTouch(SeekBar seekBar){}
		@Override
		public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
			minText.setText(progress+"%");
		}
	});
	
    //build alertdialog for minvote
	new AlertDialog.Builder(this)
    .setTitle("Votierungsforderung festlegen")
    .setView(minValView)
    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
        	//reload the fragment after changing the db with the new min value
        	groupDB.changeMinVote(translatedPosition, minSeek.getProgress());
        	onNavigationDrawerItemSelected(mCurrentSelectedPosition);
        	Log.i("votenote:changeminvote", "reloading fragment "+mCurrentSelectedPosition);
        }
    }).setNegativeButton("", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {}
    }).show();
	return true;
}
*/
