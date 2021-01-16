package com.example.storage

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.*
import com.example.storage.databinding.ActivityMainBinding
import java.io.File
import java.io.OutputStreamWriter
import java.lang.Exception
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TEXT_KEY = "TEXT_KEY"
    }
    private val binding: ActivityMainBinding by lazy {
        val tmpBinding = ActivityMainBinding.inflate(layoutInflater)
        tmpBinding
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val str = binding.editText.text
        val prefs = getPreferences(MODE_PRIVATE)

        binding.buttonSaveToPrefs.setOnClickListener{
            prefs.edit().apply {
                putString(TEXT_KEY, str.toString())
                apply()
                if(str.isNotEmpty()) {
                    toastSaved()
                }
            }
            str.clear()
        }

        binding.buttonLoadFromPrefs.setOnClickListener {

            binding.text.text = prefs.getString(TEXT_KEY, "")
            if(binding.text.text.isNotEmpty()) {
                toastLoaded()
            }
        }


        val mySavedString = File(filesDir, "save.txt")

        binding.buttonSaveInternal.setOnClickListener {
            try {
                val outputStream = mySavedString.outputStream()
                outputStream.write(str.toString().toByteArray())
                outputStream.close()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            toastSaved()
            str.clear()
        }

        binding.buttonLoadInternal.setOnClickListener {
            try {
                val inputSystem = mySavedString.inputStream()
                binding.text.text = inputSystem.readBytes().decodeToString()
                inputSystem.close()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            toastLoaded()
        }





        val db = Room.databaseBuilder(this, MyMessagesDataBase::class.java, "database")
                .fallbackToDestructiveMigration()
                .build()

        val post: MutableLiveData<String?> by lazy { MutableLiveData() }

        binding.buttonSaveToDB.setOnClickListener{
            thread {
                db.getMessagesDao().insertMessage(Message(message = str.toString()))
            }
            toastSaved()
            str.clear()
        }

        binding.buttonLoadFromDB.setOnClickListener{

            thread {
                post.postValue(db.getMessagesDao().getMessage()?.message)
            }
            binding.text.text = post.value.toString()
            toastLoaded()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(TEXT_KEY, binding.text.text.toString())
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.getString(TEXT_KEY)?.let {
            binding.text.text = it
        }
        super.onRestoreInstanceState(savedInstanceState)
    }

    private fun toastSaved () {
        Toast.makeText(this@MainActivity, "Text saved", Toast.LENGTH_SHORT).show()
    }

    private fun toastLoaded () {
        Toast.makeText(this@MainActivity, "Text loaded", Toast.LENGTH_SHORT).show()
    }
}

@Entity(tableName = "MessagesTable")
data class Message (
        @PrimaryKey(autoGenerate = true)
        val id: Int = 0,
        val message: String
)

@Dao
abstract class MessagesDao {

    @Insert
    abstract fun insertMessage(message: Message)

    @Query("SELECT * FROM MessagesTable")
    abstract fun getMessage(): Message?
}

@Database(entities = [Message::class], version = 1)
abstract class MyMessagesDataBase : RoomDatabase() {
    abstract fun getMessagesDao(): MessagesDao
}