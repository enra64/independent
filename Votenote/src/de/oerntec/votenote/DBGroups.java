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
	 * Adds a group to the Table, if it does not exist;
	 * if a group with the given name exists, we change it.
	 * @param groupName Name of the group.
	 */
	public int changeOrAddGroupName(String groupName, int minVot){
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
	 * Return a cursor containing all Groups sorted by name desc;
	 * id first, then the names
	 * @return the cursor
	 */
	public Cursor allGroupNames(){
		//sort cursor by name to have a defined reihenfolg
		String[] cols = new String[] {ID_COLUMN, UEBUNG_TYP_COLUMN};
		Cursor mCursor = database.query(true, TABLE, cols, null, null, null, null, ID_COLUMN+" DESC", null);  
		if (mCursor != null)
			mCursor.moveToFirst();
		return mCursor; // iterate to get each value.
	}
	
	public Cursor allGroupNamesAndMinvotes(){
		//sort cursor by name to have a defined reihenfolg
		String[] cols = new String[] {ID_COLUMN, UEBUNG_TYP_COLUMN, UEBUNG_MINVOTE_COLUMN};
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
	
	public int getMinVote(int group){
		String[] cols = new String[] {ID_COLUMN, UEBUNG_MINVOTE_COLUMN};
		String[] whereArgs = new String[] {String.valueOf(group)};
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

	public void changeMinVote(int group, int minValue) {
		Log.i("DBGroups", "changing minvalue");
		//create values for insert or update
		ContentValues values = new ContentValues();
		values.put(UEBUNG_MINVOTE_COLUMN, minValue);
		
		String[] whereArgs={String.valueOf(group)};
		database.update(TABLE, values, ID_COLUMN+"=?", whereArgs);
	}

	public int changePositionTO(int changePosition, String newName) {
		//get name at position in cursor
		String[] cols = new String[] {ID_COLUMN, UEBUNG_TYP_COLUMN};
		Cursor mCursor = database.query(true, TABLE, cols, null, null, null, null, ID_COLUMN+" DESC", null);  
		if (mCursor != null)
			mCursor.moveToFirst();
		mCursor.moveToPosition(changePosition);
		int changeNameAtId=mCursor.getInt(0);
		mCursor.close();
		ContentValues values = new ContentValues();
		values.put(UEBUNG_TYP_COLUMN, newName);
		
		String[] whereArgs={String.valueOf(changeNameAtId)};
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
}

