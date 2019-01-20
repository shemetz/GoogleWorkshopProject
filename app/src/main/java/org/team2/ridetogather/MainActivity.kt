package org.team2.ridetogather

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import com.facebook.AccessToken
import com.facebook.GraphRequest
import com.facebook.GraphResponse
import com.facebook.HttpMethod
import com.facebook.login.LoginManager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.team2.ridetogather.fragments.AllEvents
import org.team2.ridetogather.fragments.GroupsFragment
import org.team2.ridetogather.fragments.MyRides
import org.w3c.dom.Text


class MainActivity : AppCompatActivity() {
    private val tag = MainActivity::class.java.simpleName
    internal var mSectionsPagerAdapter: SectionsPagerAdapter?=null

    /**
     * The [ViewPager] that will host the section contents.
     */
    internal var mViewPager: ViewPager?=null
    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {

            if(position==0)
                return GroupsFragment();
            if(position==1)
                return AllEvents();
            if(position==2)
                return MyRides();

            return PlaceholderFragment.newInstance(position + 1)

        }

        override fun getCount(): Int {
            // Show 3 total pages.
            return imageResId.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
           return pageTitles.get(position)
        }
    }


    class PlaceholderFragment : Fragment() {


        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val rootView = inflater.inflate(R.layout.fragment_main, container, false)
           val  sectionLabel:TextView = rootView.findViewById(R.id.section_label)
            if (savedInstanceState != null) {
                val number = savedInstanceState.getInt(ARG_SECTION_NUMBER)
               sectionLabel.text = "This is Fragment number : "
            }
            return rootView
        }

        companion object {
            /**
             * The fragment argument representing the section number for this
             * fragment.
             */
            private val ARG_SECTION_NUMBER = "section_number"

            /**
             * Returns a new instance of this fragment for the given section
             * number.
             */
            fun newInstance(sectionNumber: Int): PlaceholderFragment {
                val fragment = PlaceholderFragment()
                val args = Bundle()
                args.putInt(ARG_SECTION_NUMBER, sectionNumber)
                fragment.arguments = args
                return fragment
            }
        }
    }
    private val imageResId =
        intArrayOf(R.drawable.ic_tab_group, R.drawable.ic_tab_events, R.drawable.ic_tab_rides)

    private val pageTitles= arrayOf("Ride Groups","Events","My Rides");
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Database.initialize(this)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        Log.d(tag, "Created $tag")
        val availability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        when (availability) {
            ConnectionResult.SUCCESS -> {
            }
            ConnectionResult.SERVICE_MISSING,
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED,
            ConnectionResult.SERVICE_DISABLED -> {
                Log.e(tag, "Google API is not available! availability = $availability")
                GoogleApiAvailability.getInstance().getErrorDialog(this, availability, 17)  // 17 is not important
            }
            else -> {
                Log.e(tag, "Google API is not available! availability = $availability")
                Log.e(tag, "Exiting app now (google maps won't work)!")
                finish()
            }
        }

        // MOCK
        temp_button_switch_user.setOnClickListener {
            Database.idOfCurrentUser = if (Database.idOfCurrentUser == MockData.user1.id)
                MockData.user2.id
            else
                MockData.user1.id
            val intent = Intent(applicationContext, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

//        val listView = findViewById<ListView>(R.id.events_list)
//        val event_request = GraphRequest.newMeRequest(
//            AccessToken.getCurrentAccessToken()
//        ) { Json, response ->
//            try {
//                Log.i(tag, "working")
//                Log.i(tag, Json.toString(4))
//                val eventsArray = Json.getJSONObject("events").getJSONArray("data")
//                val eventsNameList = arrayOfNulls<String>(eventsArray.length())
//                for (i in 0..(eventsArray.length() - 1)) {
//                    val eventName = eventsArray.optJSONObject(i).getString("name")
//                    Log.i(tag, "Event name = $eventName")
//                    eventsNameList[i] = eventName
//                }
//                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, eventsNameList)
//                listView.adapter = adapter
//                listView.setOnItemClickListener { parent, view, position, id ->
//                    val intent = Intent(applicationContext, EventRidesActivity::class.java)
//                    // MOCK
//                    val eventID = MockData.event1.id
//                    intent.putExtra(Keys.EVENT_ID.name, eventID)
//                    startActivity(intent)
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
//
//        val parameters = Bundle()
//        parameters.putString("fields", "events")
//        event_request.parameters = parameters
//        event_request.executeAsync()

        val eventID_request = GraphRequest.newGraphPathRequest(
            AccessToken.getCurrentAccessToken(),
            "/{event-id}", GraphRequest.Callback() { response: GraphResponse ->
                try {
                    Log.i(tag, "working")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })


        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.pager) as ViewPager
        mViewPager?.setAdapter(mSectionsPagerAdapter)
        val tabLayout = findViewById(R.id.tabs) as TabLayout
        tabLayout.setupWithViewPager(mViewPager)
        ///
        for (i in 0 until tabLayout.tabCount) {
            tabLayout.getTabAt(i)!!.setIcon(imageResId[i])
        }
        ///////

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(applicationContext, SettingsActivity::class.java)
                startActivity(intent)
                true
            }

            R.id.log_out_facebook -> {
                Log.d("FBLOGIN", "in log out")
                buildDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    }

    private fun disconnectFromFacebook() {
        val prefManager = PrefManager(this)
        prefManager.thisUserId = -1
        Database.idOfCurrentUser = -1
        if (AccessToken.getCurrentAccessToken() == null) {
            return  // already logged out
        }

        GraphRequest(AccessToken.getCurrentAccessToken(), "/me/permissions/", null, HttpMethod.DELETE,
            GraphRequest.Callback {
                LoginManager.getInstance().logOut()
                goToFacebookLoginActivity()
            }).executeAsync()
    }

    private fun goToFacebookLoginActivity() {
        // Go to MainActivity and start it
        val intent = Intent(applicationContext, FacebookLoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun buildDialog() {
        val builder = AlertDialog.Builder(this, R.style.AlertDialogStyle)
        //builder.setTitle("Hey there")
        builder.setMessage("Are you sure you want to log out?")
        //builder.setPositiveButton("OK", DialogInterface.OnClickListener(function = x))

        builder.setPositiveButton(R.string.leave) { dialog, which ->
            disconnectFromFacebook()
        }
        builder.setNegativeButton(R.string.stay) { dialog, which ->
            dialog.dismiss()
        }
        builder.show()

    }
}
