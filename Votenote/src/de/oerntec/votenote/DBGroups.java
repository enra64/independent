package de.oerntec.votenote;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DBGroups{
	private DatabaseCreator dbHelper;  
	
	private SQLiteDatabase database;  
	
	/* Database creation sql statement from database helper
	 * We need
	 * -key: _id
	 * -which uebung_typ: typ_uebung
	 * -which uebung_number: nummer_uebung
	 * -maximum votierungs: max_votierung
	 * -my votierungs: my_votierung
	 */
	
	public final static int NO_GROUPS_EXIST=-1;
	
	public final static String TABLE="uebungen_gruppen"; // name of table 
	
	public final static String ID_COLUMN="_id"; // id value for employee
	
	public final static String UEBUNG_TYP_COLUMN=DatabaseCreator.GROUPS_NAMEN;  // name of employee
	public final static String UEBUNG_MINVOTE_COLUMN=DatabaseCreator.GROUPS_MIN_VOTE;  // name of employee
	public final static String UEBUNG_PRESPOINTS_COLUMN=DatabaseCreator.GROUPS_PRESENTATIONPOINTS;  // name of employee
	public final static String UEBUNG_MIN_PRESPOINTS_COLUMN=DatabaseCreator.GROUPS_MIN_PRESENTATIONPOINTS;  // name of employee
	
	public DBGroups(Context context){
	    dbHelper = new DatabaseCreator(context);
	    database = dbHelper.getWritableDatabase();
	}
	
	/**
	 * Delete the group with the given name
	 * @param groupName Name of the group to delete
	 * @return Number of affected rows.
	 */
	public int deleteRecord(String groupName, int groupId){
		Log.i("dbgroups:delete", "deleted "+groupName+" at "+groupId);
		String whereClause = UEBUNG_TYP_COLUMN+"=?"+" AND "+ID_COLUMN+"=?";
		String[]whereArgs = new String[] {groupName, String.valueOf(groupId)};
		return database.delete(TABLE, whereClause , whereArgs);
	}
	
	/**
	 * Adds a group to the Table, if it does not exist;
	 * if a group with the given name exists, we change it.
	 * @param groupName Name of the group.
	 */
	public int changeOrAddGroupName(String groupName){
		//check whether group name exists; abort if it does
		String[] testColumns = new String[] {ID_COLUMN, UEBUNG_TYP_COLUMN};
		Cursor testCursor = database.query(true, TABLE, testColumns, null, null, null, null, ID_COLUMN+" DESC", null);  
		if (testCursor != null)
			testCursor.moveToFirst();
		while(testCursor.moveToNext()){
			if(testCursor.getString(1).equals(groupName))
				return -1;
		}
		//get a cursor with the id of the given groupName
		String[] cols = new String[] {ID_COLUMN};
		String[] whereArgs={groupName};
		Cursor mCursor = database.query(true, TABLE, cols, UEBUNG_TYP_COLUMN+"=?", whereArgs, null, null, null, null);
		//init cursor
		if (mCursor != null)
			if(!mCursor.moveToFirst())
				Log.w("db:groups", "empty cursor");
			
		//create values for insert or update
		ContentValues values = new ContentValues();
		values.put(UEBUNG_TYP_COLUMN, groupName);
		
		//name already exists->update
		if(mCursor.getCount()==1){
			Log.i("DBGroups", "changing entry");
			int existingId=mCursor.getInt(0);
			String[] whereArgsUpdate={String.valueOf(existingId)};
			database.update(TABLE, values, ID_COLUMN+"=?", whereArgsUpdate);
		}
		//insert name, because it does not exist yet
		else{
			Log.i("DBGroups", "adding group");
			database.insert(TABLE, null, values);
		}
		//mandatory cursorclosing
		mCursor.close();
		return 1;
	}
	
	/**
	 * Adds a group with the given Parameters
	 * @param groupName Name of the new Group
	 * @param minVot minimum vote
	 * @param minPres minimum presentation points
	 * @return -1 if group exists, 1 else.
	 */
	public int addGroup(String groupName, int minVot, int minPres){
		//check whether group name exists; abort if it does
		String[] testColumns = new String[] {ID_COLUMN, UEBUNG_TYP_COLUMN};
		Cursor testCursor = database.query(true, TABLE, testColumns, null, null, null, null, ID_COLUMN+" DESC", null);  
		if (testCursor != null)
			testCursor.moveToFirst();
		while(testCursor.moveToNext()){
			if(testCursor.getString(1).equals(groupName))
				return -1;
		}
		//get a cursor with the id of the given groupName
		String[] cols = new String[] {ID_COLUMN};
		String[] whereArgs={groupName};
		Cursor mCursor = database.query(true, TABLE, cols, UEBUNG_TYP_COLUMN+"=?", whereArgs, null, null, null, null);
		//init cursor
		if (mCursor != null)
			if(!mCursor.moveToFirst())
				Log.w("db:groups", "empty cursor");
			
		//create values for insert or update
		ContentValues values = new ContentValues();
		values.put(UEBUNG_TYP_COLUMN, groupName);
		values.put(UEBUNG_MINVOTE_COLUMN, minVot);
		values.put(UEBUNG_MIN_PRESPOINTS_COLUMN, minPres);
		
		//name already exists->update
		if(mCursor.getCount()==1){
			Log.i("DBGroups", "changing entry");
			int existingId=mCursor.getInt(0);
			String[] whereArgsUpdate={String.valueOf(existingId)};
			database.update(TABLE, values, ID_COLUMN+"=?", whereArgsUpdate);
		}
		//insert name, because it does not exist yet
		else{
			Log.i("DBGroups", "adding group");
			database.insert(TABLE, null, values);
		}
		//mandatory cursorclosing
		mCursor.close();
		return 1;
	}
	
	/**
	 * This function returns the id of the group that is displayed at drawerselection in the drawer
	 * @param drawerSelection
	 * @return 
	 */
	public int translateDrawerSelectionToDBID(int drawerSelection){
		Cursor groups=allGroupNames();
		if(groups.getCount()==0)
			return NO_GROUPS_EXIST;
		groups.moveToPosition(drawerSelection);
		int translatedSection=groups.getInt(0);
		groups.close();
		return translatedSection;
	}
	
	/**
	 * Return a cursor containing all Groups sorted by id desc;
	 * sequence: ID, NAME, MINVOTE, MINPRES
	 * @return the cursor
	 */
	public Cursor allGroupNames(){
		//sort cursor by name to have a defined reihenfolg
		String[] cols = new String[] {ID_COLUMN, UEBUNG_TYP_COLUMN, UEBUNG_MINVOTE_COLUMN, UEBUNG_MIN_PRESPOINTS_COLUMN};
		Cursor mCursor = database.query(true, TABLE, cols, null, null, null, null, ID_COLUMN+" DESC", null);  
		if (mCursor != null)
			mCursor.moveToFirst();
		return mCursor; // iterate to get each value.
	}
	
	/**
	 * returns a cursor with all 
	 * @return
	 */
	public Cursor allGroupsAllInfo(){
		//sort cursor by name to have a defined reihenfolg
		String[] cols = new String[] {ID_COLUMN, UEBUNG_TYP_COLUMN, UEBUNG_MINVOTE_COLUMN, UEBUNG_MIN_PRESPOINTS_COLUMN};
		Cursor mCursor = database.query(true, TABLE, cols, null, null, null, null, ID_COLUMN+" DESC", null);  
		if (mCursor != null)
			mCursor.moveToFirst();
		return mCursor; // iterate to get each value.
	}
	
	/**
	 * Returns a cursor containing only the row with the specified id
	 * @return the cursor
	 */
	public Cursor groupAt(int id){
		//sort cursor by name to have a defined reihenfolg
		String[] cols = new String[] {ID_COLUMN, UEBUNG_TYP_COLUMN};
		String[] whereArgs = new String[] {String.valueOf(id)};
		Cursor mCursor = database.query(true, TABLE, cols, ID_COLUMN+"=?", whereArgs, null, null, null, null);  
		if (mCursor != null)
			mCursor.moveToFirst();
		return mCursor; // iterate to get each value.
	}
	


	public int changePositionTO(int changePosition, String newName) {
		//get name at position in cursor
		String[] cols = new String[] {ID_COLUMN, UEBUNG_TYP_COLUMN};
		Cursor mCursor = database.query(true, TABLE, cols, null, null, null, null, ID_COLUMN+" DESC", null);  
		if (mCursor != null)
			mCursor.moveToFirst();
		mCursor.moveToPosition(changePosition);
		int idOfNameToChange=mCursor.getInt(0);
		mCursor.close();
		ContentValues values = new ContentValues();
		values.put(UEBUNG_TYP_COLUMN, newName);
		
		String[] whereArgs={String.valueOf(idOfNameToChange)};
		int affectedRows=database.update(TABLE, values, ID_COLUMN+"=?", whereArgs);
		Log.i("dbentries:changegroupname", "changed "+affectedRows+" entries");
		return 1;
	}
	
	public void deleteGroupAtPos(int deletePosition) {
		//get name at position in cursor
		String[] cols = new String[] {ID_COLUMN, UEBUNG_TYP_COLUMN};
		Cursor mCursor = database.query(true, TABLE, cols, null, null, null, null, ID_COLUMN+" DESC", null);
		if (mCursor != null)
			mCursor.moveToFirst();
		mCursor.moveToPosition(deletePosition);
		//get name and id for safety reasons
		int deleteId=mCursor.getInt(0);
		String deleteName=mCursor.getString(1);
		mCursor.close();

		deleteRecord(deleteName, deleteId);
	}
	
	/**
	 * Returns the minimum Vote needed for passing from db
	 * @param dbID ID of group
	 * @return minvote
	 */
	public int getMinVote(int dbID){
		String[] cols = new String[] {ID_COLUMN, UEBUNG_MINVOTE_COLUMN};
		String[] whereArgs = new String[] {String.valueOf(dbID)};
		Cursor mCursor = database.query(true, TABLE, cols, ID_COLUMN+"=?", whereArgs, null, null, null, null);  
		if (mCursor != null)
			mCursor.moveToFirst();
		//protect from exception
		if(mCursor.getCount()==0)
			return NO_GROUPS_EXIST;
		int minVote=mCursor.getInt(1);
		mCursor.close();
		return minVote;
	}
	
	/**
	 * Set the minimum amount of votes needed for passing class
	 * @param databaseID ID of the group concerned
	 * @param minValue Minimum votes
	 * @return Affected row count
	 */
	public int setMinVote(int databaseID, int minValue) {
		Log.i("DBGroups", "changing minvalue");
		//create values for insert or update
		ContentValues values = new ContentValues();
		values.put(UEBUNG_MINVOTE_COLUMN, minValue);
		
		String[] whereArgs={String.valueOf(databaseID)};
		return database.update(TABLE, values, ID_COLUMN+"=?", whereArgs);
	}
	
	/**
	 * set the presentation points
	 * @param dbID Database id of group to change
	 * @param presPoints presentation points the user has
	 * @return The amount of Rows updated, should be one
	 */
	public int setPresPoints(int dbID, int presPoints){
		String[] whereArgs={String.valueOf(dbID)};
		ContentValues values = new ContentValues();
		values.put(UEBUNG_PRESPOINTS_COLUMN, presPoints);
		return database.update(TABLE, values, ID_COLUMN+"=?", whereArgs);
	}
	
	/**
	 * Helper function for getting the pres points for the specified group
	 * @param dbID the database id of the group
	 * @return Prespoint number
	 */
	public int getPresPoints(int dbID) {
		String[] cols = new String[] {ID_COLUMN, UEBUNG_PRESPOINTS_COLUMN};
		String[] whereArgs = new String[] {String.valueOf(dbID)};
		Cursor mCursor = database.query(true, TABLE, cols, ID_COLUMN+"=?", whereArgs, null, null, ID_COLUMN+" DESC", null);
		if (mCursor != null)
			mCursor.moveToFirst();
		int presPoints=mCursor.getInt(1);
		mCursor.close();
		return presPoints;
	}
	
	/**
	 * set the minimum amount of presentation points needed for passing
	 * @param dbID Database id of group to change
	 * @param minPresPoints presentation points the user has
	 * @return The amount of rows updated, should be one
	 */
	public int setMinPresPoints(int dbID, int minPresPoints){
		Log.i("dbgroups", "setting min pres points");
		String[] whereArgs={String.valueOf(dbID)};
		ContentValues values = new ContentValues();
		values.put(UEBUNG_MIN_PRESPOINTS_COLUMN, minPresPoints);
		return database.update(TABLE, values, ID_COLUMN+"=?", whereArgs);
	}
	
	/**
	 * Get the minimum prespoints for the given group
	 * @param dbID ID of the Group
	 * @return Minimum number of Prespoints needed for passing
	 */
	public int getMinPresPoints(int dbID) {
		String[] cols = new String[] {ID_COLUMN, UEBUNG_MIN_PRESPOINTS_COLUMN};
		String[] whereArgs = new String[] {String.valueOf(dbID)};
		Cursor mCursor = database.query(true, TABLE, cols, ID_COLUMN+"=?", whereArgs, null, null, ID_COLUMN+" DESC", null);
		if (mCursor != null)
			mCursor.moveToFirst();
		int maxPresPoints=mCursor.getInt(1);
		mCursor.close();
		return maxPresPoints;
	}
}

