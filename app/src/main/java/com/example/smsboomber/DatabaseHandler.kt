import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.smsboomber.Message

class DatabaseHandler(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "MessageDatabase"
        private const val TABLE_NAME = "messages"
        private const val KEY_NUMBER = "number"
        private const val KEY_MESSAGE = "message"
        private const val KEY_MESSAGE_INDEX = "message_index"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = "CREATE TABLE $TABLE_NAME ($KEY_NUMBER TEXT, $KEY_MESSAGE TEXT, $KEY_MESSAGE_INDEX TEXT)"
        db?.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun addMessage(number: String, message: String, messageIndex: String): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(KEY_NUMBER, number)
        contentValues.put(KEY_MESSAGE, message)
        contentValues.put(KEY_MESSAGE_INDEX, messageIndex)

        val result = db.insert(TABLE_NAME, null, contentValues)
        db.close()
        return result
    }

    fun getAllMessages(): ArrayList<Message> {
        val messageList = ArrayList<Message>()
        val selectQuery = "SELECT * FROM $TABLE_NAME"
        val db = this.readableDatabase
        val cursor: Cursor?

        cursor = db.rawQuery(selectQuery, null)

        if (cursor != null) {
            val numberIndex = cursor.getColumnIndex(KEY_NUMBER)
            val messageIndex = cursor.getColumnIndex(KEY_MESSAGE)
            val messageIndexIndex = cursor.getColumnIndex(KEY_MESSAGE_INDEX)

            while (cursor.moveToNext()) {
                val number = if (numberIndex != -1) cursor.getString(numberIndex) else ""
                val message = if (messageIndex != -1) cursor.getString(messageIndex) else ""
                val messageIndex = if (messageIndexIndex != -1) cursor.getString(messageIndexIndex) else ""

                val messageObject = Message(number, message, messageIndex)
                messageList.add(messageObject)
            }
            cursor.close()
        }
        db.close()
        return messageList
    }

    fun deleteAllMessages() {
        val db = this.writableDatabase
        db.delete(TABLE_NAME, null, null)
        db.close()
    }
}

