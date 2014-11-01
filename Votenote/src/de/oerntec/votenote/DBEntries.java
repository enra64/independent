package de.oerntec.votenote;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DBEntries{  

private DatabaseCreator dbHelper;  

private SQLiteDatabase database;  

public final static String TABLE=DatabaseCreator.TABLE_NAME_ENTRIES; // name of table 

/* Database creation sql statement from database helper
 * We need
 * -key: _id
 * -which uebung_typ: typ_uebung
 * -which uebung_number: nummer_uebung
 * -maximum votierungs: max_votierung
 * -my votierungs: my_votierung
 */

public final static String ID_COLUMN=DatabaseCreator.ENTRIES_ID; // id value for employee

public final static String UEBUNG_TYP_COLUMN=DatabaseCreator.ENTRIES_TYP_UEBUNG;  //name of the uebung
public final static String UEBUNG_NUMMER_COLUMN=DatabaseCreator.ENTRIES_NUMMER_UEBUNG;  //which iteration of the uebung is it
public final static String MAX_VOTE_NUMBER_COLUMN=DatabaseCreator.ENTRIES_MAX_VOTES;  //max possible vote count
public final static String MY_VOTE_NUMBER_COLUMN=DatabaseCreator.ENTRIES_MY_VOTES;  //my vote count
/** 
 * 
 * @param context 
 */  
public DBEntries(Context context){  
    dbHelper = new DatabaseCreator(context);  
    database = dbHelper.getWritableDatabase();  
}

/**
 * Adds a group to the Table, if it does not exist;
 * if a group with the given name exists, we change it.
 * @param groupName Name of the group.
 */
public void changeOrAddEntry(String uebungTyp, int maxVote, int myVote){
	//get a cursor with all entries
	String[] cols = new String[] {ID_COLUMN, UEBUNG_TYP_COLUMN, UEBUNG_TYP_COLUMN};
	Cursor mCursor = database.query(true, TABLE, cols, null, null, null, null, null, null);
	
	//create values for insert or update
	ContentValues values = new ContentValues();
	values.put(UEBUNG_TYP_COLUMN, uebungTyp);
	values.put(UEBUNG_NUMMER_COLUMN, 12);
	values.put(MAX_VOTE_NUMBER_COLUMN, maxVote);
	values.put(MY_VOTE_NUMBER_COLUMN, myVote);
	
	//name already exists->update
	if(mCursor.getCount()==1){
		int existingId=mCursor.getInt(0);
		String[] whereArgs={String.valueOf(existingId)};
		database.update(TABLE, values, ID_COLUMN+"=?", whereArgs);
	}
	//insert name, because it does not exist yet
	else
		database.insert(TABLE, null, values);
	//mandatory cursorclosing
	mCursor.close();
}

public void changeEntry(int uebungTyp, int uebungNummer, int maxVote, int myVote){
	//create values for insert or update
	ContentValues values = new ContentValues();
	values.put(MAX_VOTE_NUMBER_COLUMN, maxVote);
	values.put(MY_VOTE_NUMBER_COLUMN, myVote);
	
	String[] whereArgs={String.valueOf(uebungTyp), String.valueOf(uebungNummer)};
	int affectedRows=database.update(TABLE, values, UEBUNG_TYP_COLUMN+"=?"+" AND "+UEBUNG_NUMMER_COLUMN+"=?", whereArgs);
	Log.i("dbentries:changeentry", "changed "+affectedRows+" entries");
}

public Cursor getEntry(int uebungTyp, int uebungNummer){
	String[] cols = new String[] {MY_VOTE_NUMBER_COLUMN, MAX_VOTE_NUMBER_COLUMN, ID_COLUMN};
	String[] whereArgs={String.valueOf(uebungTyp), String.valueOf(uebungNummer)};
	Cursor mCursor = database.query(true, TABLE, cols, UEBUNG_TYP_COLUMN+"=?"+" AND "+UEBUNG_NUMMER_COLUMN+"=?", whereArgs, null, null, null, null);  
	if (mCursor != null)
		mCursor.moveToFirst();
	return mCursor; // iterate to get each value.
}

public void deleteAllEntriesForGroup(int groupId) {
	String whereClause = UEBUNG_TYP_COLUMN+"=?";
	String[] whereArgs = new String[] {String.valueOf(groupId)};
	int checkValue=database.delete(TABLE, whereClause , whereArgs);
	Log.i("dbgroups:delete", "deleting all "+checkValue+" entries of type "+groupId);
}

/**
 * Add an entry to the respective uebung
 * @param uebungTyp
 * @param maxVote
 * @param myVote
 */
public void addEntry(int uebungTyp, int maxVote, int myVote){
	Cursor lastEntryNummerCursor = database.query(true, TABLE, new String[] {UEBUNG_NUMMER_COLUMN}, UEBUNG_TYP_COLUMN+"="+uebungTyp, null, null, null, UEBUNG_NUMMER_COLUMN+" DESC", null);
	//init cursor
	int lastNummer=1;
	if (lastEntryNummerCursor != null){
		if(!lastEntryNummerCursor.moveToFirst())
			Log.w("db:groups", "empty cursor");
	}
	if(lastEntryNummerCursor.getCount()!=0)
		lastNummer=lastEntryNummerCursor.getInt(0)+1;
	
	Log.i("db:entries:add", "adding entry with lastnummer"+lastNummer+" for group "+uebungTyp);
	
	//create values for insert or update
	ContentValues values = new ContentValues();
	values.put(UEBUNG_TYP_COLUMN, uebungTyp);
	values.put(UEBUNG_NUMMER_COLUMN, lastNummer);
	values.put(MAX_VOTE_NUMBER_COLUMN, maxVote);
	values.put(MY_VOTE_NUMBER_COLUMN, myVote);
	
	database.insert(TABLE, null, values);
}

/**
 * Return a cursor for all entries
 * @return the cursor
 */
public Cursor getAllRecords(){
	String[] cols = new String[] {UEBUNG_NUMMER_COLUMN, MY_VOTE_NUMBER_COLUMN, MAX_VOTE_NUMBER_COLUMN, ID_COLUMN};
	Cursor mCursor = database.query(true, TABLE, cols, null, null, null, null, null, null);  
	if (mCursor != null)
		mCursor.moveToFirst();  
	return mCursor; // iterate to get each value.
}

public int getPrevMaxVote(int groupID){
	String[] cols = new String[] {ID_COLUMN, MAX_VOTE_NUMBER_COLUMN};
	String[] whereArgs = new String[] {String.valueOf(groupID)};
	Cursor mCursor = database.query(true, TABLE, cols, UEBUNG_TYP_COLUMN+"=?", whereArgs, null, null, UEBUNG_NUMMER_COLUMN+" DESC", null);  
	if (mCursor != null)
		mCursor.moveToFirst();
	if(mCursor.getCount()!=0)
		return mCursor.getInt(1);
	else
		return 10;
}

/**
 * Return a cursor for the selected group_type
 * {UEBUNG_NUMMER_COLUMN, MY_VOTE_NUMBER_COLUMN, MAX_VOTE_NUMBER_COLUMN, ID_COLUMN}
 * @return the cursor
 */
public Cursor getGroupRecords(int groupType){
	String[] cols = new String[] {UEBUNG_NUMMER_COLUMN, MY_VOTE_NUMBER_COLUMN, MAX_VOTE_NUMBER_COLUMN, ID_COLUMN};
	String[] whereArgs = new String[] {String.valueOf(groupType)};
	Cursor mCursor = database.query(true, TABLE, cols, UEBUNG_TYP_COLUMN+"=?", whereArgs, null, null, null, null);  
	if (mCursor != null)
		mCursor.moveToFirst();  
	return mCursor; // iterate to get each value.
}
}
