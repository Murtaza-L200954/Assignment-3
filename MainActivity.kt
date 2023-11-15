package com.example.contacts

import android.R
import android.app.DownloadManager.Query
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.view.View
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.contacts.ui.theme.ContactsTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ContactsTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Greeting("Android")
                }
            }
        }
    }
}
@Entity(tableName = "contacts")
public class Contact {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;

    private String phoneNumber;

    // Getters and setters
}

@Dao
interface ContactDao {
    @get:Query("SELECT * FROM contacts")
    val allContacts: List<Contact?>?

    @Insert
    fun insertContact(contact: Contact?)

    @Update
    fun updateContact(contact: Contact?)

    @Delete
    fun deleteContact(contact: Contact?)
}

@Database(entities = [Contact::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao?

    companion object {
        private var instance: AppDatabase? = null
        @Synchronized
        fun getInstance(context: Context): AppDatabase? {
            if (instance == null) {
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java, "contact_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
            }
            return instance
        }
    }
}

class MainActivity : AppCompatActivity() {
    private var contactDao: ContactDao? = null
    protected fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Room database
        contactDao = AppDatabase.getInstance(this)!!.contactDao()
        val importContactsButton: Button = findViewById(R.id.importContactsButton)
        importContactsButton.setOnClickListener { view: View? -> importContacts() }
    }

    private fun importContacts() {
        // Use ContentResolver to query contacts
        val contentResolver: ContentResolver = getContentResolver()
        val cursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null,
            null,
            null,
            null
        )
        if (cursor != null && cursor.count > 0) {
            while (cursor.moveToNext()) {
                val name = cursor.getString(
                    cursor.getColumnIndex(
                        ContactsContract.Contacts.DISPLAY_NAME
                    )
                )

                // Assuming you want the first phone number of the contact
                val phoneNumber = getPhoneNumber(cursor)

                // Create a Contact object and insert it into the database
                val contact = Contact()
                contact.setName(name)
                contact.setPhoneNumber(phoneNumber)
                contactDao!!.insertContact(contact)
            }
            cursor.close()
        }
    }

    private fun getPhoneNumber(cursor: Cursor): String? {
        var phoneNumber: String? = null
        val contactId = cursor.getString(
            cursor.getColumnIndex(
                ContactsContract.Contacts._ID
            )
        )
        val phoneCursor: Cursor = getContentResolver().query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", arrayOf<String>(contactId),
            null
        )
        if (phoneCursor != null && phoneCursor.moveToFirst()) {
            phoneNumber = phoneCursor.getString(
                phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            )
            phoneCursor.close()
        }
        return phoneNumber
    }
}
