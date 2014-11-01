package de.oerntec.votenote;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseCreator extends SQLiteOpenHelper {
    private static final String DATABASE = "uebungen";
    
    private static final int DATABASE_VERSION = 10;

    /* Database creation sql statement
     * We need
     * -key: _id
     * -which uebung_typ: typ_uebung
     * -which uebung_number: nummer_uebung
     * -maximum votierungs: max_votierung
     * -my votierungs: my_votierung
     */
    
    public static final String ENTRIES_ID="_id";
    public static final String ENTRIES_TYP_UEBUNG="typ_uebung";
    public static final String ENTRIES_NUMMER_UEBUNG="nummer_uebung";
    public static final String ENTRIES_MAX_VOTES="max_votierung";
    public static final String ENTRIES_MY_VOTES="my_votierung";
    
    public static final String TABLE_NAME_ENTRIES="uebungen_eintraege";
    
    private static final String CREATE_DATABASE_ENTRIES = 
    		"create table "+TABLE_NAME_ENTRIES+"( "+ENTRIES_ID+" integer primary key," +
    		ENTRIES_TYP_UEBUNG+" int not null, " +
    		ENTRIES_NUMMER_UEBUNG+" integer not null," +
    		ENTRIES_MAX_VOTES+" integer not null," +
    		ENTRIES_MY_VOTES+" integer not null);";
    
    public static final String GROUPS_ID="_id";
    public static final String GROUPS_NAMEN="uebung_name";
    public static final String GROUPS_MIN_VOTE="uebung_minvote";
    public static final String GROUPS_PRESENTATIONPOINTS="uebung_prespoints";
    
    public static final String TABLE_NAME_GROUPS="uebungen_gruppen";
    
    private static final String CREATE_DATABASE_GROUPS = 
    		"create table "+TABLE_NAME_GROUPS+"( "+GROUPS_ID+" integer primary key," +
    		GROUPS_NAMEN+" string not null," +
    		GROUPS_MIN_VOTE+" integer DEFAULT 50,"+
    		GROUPS_PRESENTATIONPOINTS + " integer DEFAULT 0);";

    public DatabaseCreator(Context context) {
        super(context, DATABASE, null, DATABASE_VERSION);
    }

    // Method is called during creation of the database
    @Override
    public void onCreate(SQLiteDatabase database) {
    	database.execSQL(CREATE_DATABASE_ENTRIES);
    	database.execSQL(CREATE_DATABASE_GROUPS);
    }

    // Method is called during an upgrade of the database,
    @Override
    public void onUpgrade(SQLiteDatabase database,int oldVersion,int newVersion){
        Log.w(DatabaseCreator.class.getName(),
                         "Upgrading database from version " + oldVersion + " to "
                         + newVersion + ", which will destroy all old data");
        /*
         * more advanced db management
         */
        //rename old db, create new one;
        database.execSQL("ALTER TABLE "+TABLE_NAME_ENTRIES+" RENAME TO oldentries");
    	database.execSQL(CREATE_DATABASE_ENTRIES);
    	//copy values
        database.execSQL("INSERT INTO "+TABLE_NAME_ENTRIES+" ("
            	+ENTRIES_ID+", "+ENTRIES_TYP_UEBUNG+", "+ENTRIES_NUMMER_UEBUNG+", "+ENTRIES_MAX_VOTES+", "+ENTRIES_MY_VOTES+") "
            			+ "SELECT "
            	+ENTRIES_ID+", "+ENTRIES_TYP_UEBUNG+", "+ENTRIES_NUMMER_UEBUNG+", "+ENTRIES_MAX_VOTES+", "+ENTRIES_MY_VOTES+"  FROM oldentries");
        //drop old one
        database.execSQL("DROP TABLE IF EXISTS oldentries");
        /*
         * old db manager
         */
        database.execSQL("DROP TABLE IF EXISTS uebungen_eintraege");
        database.execSQL("DROP TABLE IF EXISTS uebungen_gruppen");
        onCreate(database);
    }
}