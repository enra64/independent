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
	
	//database connection
	private static DBGroups groupDB;
	private static DBEntries entryDB;
	
	TextView ppV;
	
	private static int mCurrentSelectedPosition;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//database access
		groupDB=new DBGroups(this);
		entryDB=new DBEntries(this);
		
		//to avoid calling groupDB before having it started, setting the view has been
		//moved here
		setContentView(R.layout.activity_main);
		
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
			mTitle="�bung hinzuf�gen!";
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
			case R.id.action_add_entry:
				createEntryDialog(groupID, 0);
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
				int prevValue=Integer.valueOf(presPointsView.getText().toString());
				presPointsView.setText(String.valueOf(prevValue+1));
			}
		});
		minusButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				int prevValue=Integer.valueOf(presPointsView.getText().toString());
				presPointsView.setText(String.valueOf(prevValue-1));
			}
		});

		/*
		 * PRESPOINT ALERTDIALOG
		 */
		Builder b=new AlertDialog.Builder(this)
	    .setView(inputView)
	    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	        	//write new prespoint count to db
	        	String presPointsString=presPointsView.getText().toString();
	            groupDB.setPresPoints(dataBaseId, Integer.valueOf(presPointsString));
	            //reload fragment
	        	onNavigationDrawerItemSelected(mCurrentSelectedPosition);
	        	Log.i("votenote:addentry", "reloading fragment "+mCurrentSelectedPosition);
	        }
	    }).setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int whichButton) {}
	    });
		b.setTitle("Erreichte Pr�sentationspunkte");
		b.create().show();
	}
	
	/**
	 * Dialog for creating an entry
	 * @param groupID
	 * @param add
	 * @param uebungNummer
	 */
	private void createEntryDialog(final int groupID, int uebungNummer){
		//adding entry, use values put in last
		int maxVoteValue=entryDB.getPrevMaxVote(groupID);
		int minVoteValue=entryDB.getPrevVote(groupID);
		
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
        infoView.setText(myVote.getValue()+" von "+maxVote.getValue()+" Votierungen");
        infoView.setTextColor(Color.argb(255, 153, 204, 0));//green
        
       //add change listener to update dialog expl. if pickers changed
        myVote.setOnValueChangedListener (new OnValueChangeListener() {
        @Override
		public void onValueChange(NumberPicker thisPicker, int arg1, int newVal) {
			int myVoteValue=newVal;
    		int maxVoteValue=maxVote.getValue();
			infoView.setText(myVoteValue+" von "+maxVoteValue+" Votierungen");
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
			infoView.setText(myVoteValue+" von "+maxVoteValue+" Votierungen");
			if(maxVoteValue>=myVoteValue)
				infoView.setTextColor(Color.argb(255, 153, 204, 0));//green
			else
				infoView.setTextColor(Color.argb(255, 204, 0, 0));//red
			}
        });
        
        //build alertdialog
		new AlertDialog.Builder(this)
	    .setTitle("Neue �bungsnotiz")
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
	        		Toast.makeText(getApplicationContext(), "Mehr votiert als m�glich!", Toast.LENGTH_SHORT).show();
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
			final int databaseID= groupDB.translateDrawerSelectionToDBID(mSectionNumber);

			//find and inflate everything
			View rootView = inflater.inflate(R.layout.fragment_main, container, false);
			ListView voteList=(ListView) rootView.findViewById(R.id.voteList);
			final TextView averageView=(TextView) rootView.findViewById(R.id.voteAverage);
			final TextView presPointView=(TextView) rootView.findViewById(R.id.mainPrespointView);
			final TextView averageVotesNeededView=(TextView) rootView.findViewById(R.id.textViewAverageNeededVotes);
			
			
			//if translatedSection is -1, no group has been added yet
			if(databaseID==DBGroups.NO_GROUPS_EXIST){
				Intent intent = new Intent(getActivity(), GroupManagementActivity.class);
				intent.putExtra("firstGroup", true);
				startActivity(intent);
			}
			else{
				Log.i("votenote main", "displaying entries for group "+databaseID+" (according to group db)");
				
				/* PRESENTATION POINTS INFO
				 * set view containing info about the current 
				 * and needed presentation points
				 */
				int presPoint=groupDB.getPresPoints(databaseID);
				int minPresPoints=groupDB.getMinPresPoints(databaseID);
				String presDescription="";
				if(minPresPoints==1)
					presDescription=" Vortrag";
				else
					presDescription=" Vortr�gen";
				presPointView.setText(presPoint+" von "+minPresPoints+presDescription);
				
				//make view invisible if no presentations are required
				if(minPresPoints==0)
					presPointView.setVisibility(View.INVISIBLE);
				
				//calculate how many work you need to do per uebung to achieve
				//minimum votation
				//need: current work done, maximum doable work, needed percentage of work
				int maximumDoableWork=groupDB.getMaxWorkForGroup(databaseID);
				int currentDoneWork = setVoteAverage(databaseID, averageView, true);
				int neededPercentage = groupDB.getMinVote(databaseID);
				
				/* ALLENTRY LISTVIEW
				 * create the listview adapter responsible for showing all group entries
				 */
				Cursor allEntryCursor=entryDB.getGroupRecords(databaseID);
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
				
				/*
				 * make onclicklistener if a entry has been clicked
				 */
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
				        
				        /*
				         * alert dialog for changing entry
				         */
				        //building alertdialog with the view just configured
						new AlertDialog.Builder(getActivity())
					    .setTitle("Eintrag �ndern")
					    .setView(pickView)
					    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					        public void onClick(DialogInterface dialog, int whichButton) {
					        	if(myVote.getValue()<=maxVote.getValue()){//check for valid entry
					        		//change db entry
					        		entryDB.changeEntry(databaseID, translatedPosition, maxVote.getValue(), myVote.getValue());
									//reload list- and textview; close old cursor
					        		groupAdapter.swapCursor(entryDB.getGroupRecords(databaseID)).close();
					        		setVoteAverage(databaseID, averageView, false);
					        	}
					        	else
					        		Toast.makeText(getActivity(), "Mehr votiert als m�glich!", Toast.LENGTH_SHORT).show();
					        }
					    }).setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
					        public void onClick(DialogInterface dialog, int whichButton) {}
					    }).show();
					}
					
					/**
					 * configures the numberpickers with the given parameters
					 */
					private void configureNumberPickers(final TextView infoView, final NumberPicker maxVote, final NumberPicker myVote, int translatedPosition) {
						//get the old values from the database; position +1 because natural counting style
						//is used when counting the uebungs
						Cursor oldValues=entryDB.getEntry(databaseID, translatedPosition);
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
				        oldValues.close();
					}
				});
				
				//set summaryView to average
				setVoteAverage(databaseID, averageView, false);
			}
			return rootView;
		}
		
		/*
		 * calculate the average of votings vs how much you need
		 */
		private int setVoteAverage(int forSection, TextView affectView, boolean onlyReturnAverage){
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
				affectView.setText("F�ge einen Eintrag ein.");
			}
			//calc percentage
			float average=((float)myVoteCount/maxVoteCount)*100;
			int avg=(int)average;
			
			//get minvote for section
			int minVote=groupDB.getMinVote(forSection);
			
			if(!onlyReturnAverage){
				//write percentage and color coding to summaryview
				affectView.setText(avg+"%");
				if(avg>=minVote)
					affectView.setTextColor(Color.argb(255, 153, 204, 0));//green
				else
					affectView.setTextColor(Color.argb(255, 204, 0, 0));//red
			}
			return avg;
		}
		
		@Override
		public void onAttach(Activity activity) {
			super.onAttach(activity);
			((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
		}
	}

	
}
