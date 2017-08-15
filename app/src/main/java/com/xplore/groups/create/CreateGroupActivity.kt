package com.xplore.groups.create

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.DatePicker
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import com.xplore.*
import com.xplore.database.DBManager
import com.xplore.reserve.ReserveInfoActivity

import java.util.ArrayList

import com.xplore.General.currentUserId
import com.xplore.groups.Group
import com.xplore.maps.MapActivity
import com.xplore.user.User
import kotlinx.android.synthetic.main.create_group.*
import kotlin.collections.HashMap

/**
 * Created by Nikaoto on 2/18/2017.

 * აღწერა:
 * ეს ფრაგმენტი იხსნება როცა მომხმარებელი ახალ გუნდს ქმნის. ეს კლასი ამოწმებს მომხმარებლის შეცდომებს
 * ფორმის შევსებისას და "დასტურზე" დაჭერის შემდგომ ტვირთავს ახალ გუნდს Firebase-ს ბაზაში

 * Description:
 * This fragment opens when user is creating a group. This class checks for any errors in user's
 * group info and with "Done" uploads group info to groups Fireabase Database

 */

class CreateGroupActivity : Activity(), DatePickerDialog.OnDateSetListener {

    //TODO replace reserveButton with reserveCard

    //Request codes
    private val SEARCH_DESTINATION_REQ_CODE = 1
    private val SELECT_FROM_MAP_REQ_CODE = 2
    private val INVITE_USERS_REQ_CODE = 4

    //Database
    private val dbManager: DBManager by lazy { DBManager(this) }

    //Firebase
    private val usersRef = FirebaseDatabase.getInstance().reference.child("users")
    private val groupsRef = FirebaseDatabase.getInstance().reference.child("groups")
    private val joinedGroupsRef = FirebaseDatabase.getInstance().reference
            .child("users")
            .child(General.currentUserId)
            .child("group_ids")

    //Limits and restrictions to fields
    private val EXPERIENCE_ANS_DEFAULT = -1
    private val EXPERIENCE_ANS_NO = 0
    private val EXPERIENCE_ANS_YES = 1
    private val G_PREFS_CHAR_MAX = 200
    private val G_PREFS_CHAR_MIN = 0 //TODO add selection if user doesn't have prefs
    private val E_INFO_CHAR_MAX = 200
    private val E_INFO_CHAR_MIN = 5

    //Tags for date and time pickers
    private val SELECTION_NONE = ""
    private val SELECTION_START = "start"
    private val SELECTION_END = "end"
    private var selecting = SELECTION_NONE

    //Setting chosen answer and destination to default
    private var chosenDestId = Group.DESTINATION_DEFAULT
    private var experienceAns = EXPERIENCE_ANS_DEFAULT

    private var destinationLat = 0.0
    private var destinationLng = 0.0

    private var groupImageUrl = ""
    private var groupName = ""
    private var groupPrefs = ""
    private var extraInfo = ""

    private var invitedMemberIds = ArrayList<String>()

    private val date = Date()

    //TODO remove dependenies and inner objects/classes as much as possible

    companion object {
        @JvmStatic
        fun getStartIntent(context: Context): Intent {
            return Intent(context, CreateGroupActivity::class.java)
        }
    }

    init {
        //Refreshing server timeStamp
        TimeManager.refreshGlobalTimeStamp()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.create_group)

        setTitle(R.string.activity_create_group_title)

        initMemberRecyclerView()
        initClickEvents()
    }

    private fun initMemberRecyclerView() {
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        invitedMemberList.setHasFixedSize(true)
        invitedMemberList.layoutManager = layoutManager
    }

    private fun initClickEvents() {

        groupNameHelpButton.setOnClickListener {
            showHelp(R.string.group_name, R.string.group_name_help, R.string.okay)
        }

        chooseDestinationButton.setOnClickListener {
            showDestinationDialog()
        }

        groupImageView.setOnClickListener {
            if (chosenDestId != Group.DESTINATION_DEFAULT) {
                startActivity(ReserveInfoActivity.getStartIntent(this, chosenDestId))
            } else if (groupImageUrl.isNotEmpty()) {
                startActivityForResult(MapActivity.getStartIntent(this, true),
                        SELECT_FROM_MAP_REQ_CODE)
            }
        }

        startDateButton.setOnClickListener {
            showDatePicker(SELECTION_START)
        }

        startTimeButton.setOnClickListener {
            showTimePicker(SELECTION_START)
        }

        endDateButton.setOnClickListener {
            showDatePicker(SELECTION_END)
        }

        endTimeButton.setOnClickListener {
            showTimePicker(SELECTION_END)
        }

        inviteButton.setOnClickListener {
            startActivityForResult(SearchUsersActivity.getStartIntent(this, invitedMemberIds),
                    INVITE_USERS_REQ_CODE)
        }

        prefs_help.setOnClickListener {
            showHelp(R.string.group_preferences, R.string.group_prefs_help, R.string.okay)
        }

        extraInfo_help.setOnClickListener {
            showHelp(R.string.extra_info, R.string.extra_info_help, R.string.okay)
        }

        doneButton.setOnClickListener {
            General.HideKeyboard(this)
            getDescriptions()
            if (checkFields()) {
                val key = groupsRef.push().key
                uploadGroupData(key)
                addLeaderToGroup(key)
                sendInvites(key)
                Toast.makeText(this, "The group has been created!", Toast.LENGTH_SHORT).show() //TODO string resources
                finish()
            }
        }

        radioGroup.setOnCheckedChangeListener { radioGroup, i ->
            if (i == R.id.yes_rb)
                experienceAns = EXPERIENCE_ANS_YES
            else if (i == R.id.no_rb)
                experienceAns = EXPERIENCE_ANS_NO
        }
    }

    private fun showDestinationDialog() {
        AlertDialog.Builder(this)
                .setTitle(R.string.activity_choose_destination_title)
                .setMessage(R.string.choose_from)
                .setPositiveButton(R.string.activity_library_title) {_, _ ->
                    startActivityForResult(
                            Intent(this@CreateGroupActivity, SearchDestinationActivity::class.java),
                            SEARCH_DESTINATION_REQ_CODE)
                }
                .setNegativeButton(R.string.activity_maps_title) {_, _ ->
                    startActivityForResult(
                            MapActivity.getStartIntent(this, true),
                            SELECT_FROM_MAP_REQ_CODE)
                }
                .create().show()
    }

    private fun showDatePicker(code: String) {
        if (TimeManager.globalTimeStamp != 0L) {
            selecting = code
            DatePickerFragment(this@CreateGroupActivity, TimeManager.globalTimeStamp, 0)
                    .show(fragmentManager, "")
        }
    }

    override fun onDateSet(view: DatePicker, year: Int, receivedMonth: Int, day: Int) {
        val month  = receivedMonth + 1 //+1 is necessary because 0 is January
        when (selecting) {
            SELECTION_START -> {
                selecting = SELECTION_NONE
                date.setStartDate(year, month, day)
                startDateTextView.text = date.getStartDateString()
            }
            SELECTION_END -> {
                selecting = SELECTION_NONE
                date.setEndDate(year, month, day)
                endDateTextView.text = date.getEndDateString()
            }
        }
    }

    private fun showTimePicker(code: String) {
        selecting = code
        TimePickerFragment(onTimeSetListener).show(fragmentManager, "")
    }

    private val onTimeSetListener = TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
        when (selecting) {
            SELECTION_START -> {
                selecting = SELECTION_NONE
                date.setStartTime(hourOfDay, minute)
                startTimeTextView.text = date.getStartTimeText()
            }
            SELECTION_END -> {
                selecting = SELECTION_NONE
                date.setEndTime(hourOfDay, minute)
                endTimeTextView.text = date.getEndTimeText()
            }
        }
    }

    public override fun onResume() {
        super.onResume()

        if (!General.isNetConnected(this)) {
            General.createNetErrorDialog(this)
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null) {
            super.onActivityResult(requestCode, resultCode, data)

            when (requestCode) {
                SEARCH_DESTINATION_REQ_CODE ->
                    if (resultCode == Activity.RESULT_OK) {
                        chosenDestId = data.getIntExtra("chosen_destination_id",
                                Group.DESTINATION_DEFAULT)

                        dbManager.openDataBase()
                        Picasso.with(this)
                                .load(dbManager.getImageId(chosenDestId))
                                .into(groupImageView)
                        dbManager.close()
                    }

                SELECT_FROM_MAP_REQ_CODE ->
                    if (resultCode == Activity.RESULT_OK) {
                        chosenDestId = Group.DESTINATION_DEFAULT

                        //Getting image
                        destinationLat = data.getDoubleExtra(MapActivity.RESULT_DEST_LAT, 0.0)
                        destinationLng = data.getDoubleExtra(MapActivity.RESULT_DEST_LNG, 0.0)

                        //Checking if data retrieval failed
                        if (destinationLat != 0.0 && destinationLng != 0.0) {
                            groupImageUrl = MapUtil.getMapUrl(destinationLat, destinationLng)

                            Picasso.with(this).load(groupImageUrl).into(groupImageView)
                        } else {
                            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show()
                        }
                    }

                INVITE_USERS_REQ_CODE ->
                    if (resultCode == Activity.RESULT_OK) {
                        invitedMemberIds = data.getStringArrayListExtra("invitedMemberIds")
                        populateMembersList(invitedMemberIds)
                    }
            }
        }
    }

    fun ArrayList<String>.toMap(): HashMap<String, Boolean> {
        val temp = HashMap<String, Boolean>(this.size)
        for(item in this) {
            temp.put(item, true)
        }
        return temp
    }

    //Creates uploadable group
    private fun createUploadGroup(key: String): UploadableGroup {
        //Member ids of the group
        val member_ids = HashMap<String, Boolean>(1)
        member_ids.put(currentUserId, true) //Adding leader to members list

        //get experience question
        val exp = experienceAns != EXPERIENCE_ANS_NO

        return UploadableGroup(
                key, //Firebase Unique Group Key
                groupName,
                exp, //Group Experienced Boolean
                date.getStartDate(), //Start Date
                date.startTime, //Start Time
                date.getEndDate(), //End Date
                date.endTime,
                chosenDestId, //Chosen Destination Id
                destinationLat, destinationLng, //Destination location
                groupImageUrl, //Image url
                extraInfo, //Group Extra Info
                groupPrefs, //Group Preferences
                member_ids, //Group Member Ids (only the leader)
                invitedMemberIds.toMap()) //Invited members
    }

    private fun addLeaderToGroup(key: String) {
        joinedGroupsRef.child("/" + key).setValue(true)
    }

    private fun uploadGroupData(key: String) {
        //Group
        val groupData = createUploadGroup(key).toMap()
        val groupUpdates = HashMap<String, Any>()
        groupUpdates.put("/" + key, groupData)
        groupsRef.updateChildren(groupUpdates)
    }

    private fun sendInvites(groupId: String) {
        for (memberId in invitedMemberIds) {
            usersRef.child(memberId).child("invited_group_ids").child("/" + groupId).setValue(true)
        }
    }

    private fun populateMembersList(memberIds: ArrayList<String>) {
        val membersToDisplay = ArrayList<User>(memberIds.size)
        var memberCount = memberIds.size
        for (memberId in memberIds) {
            usersRef.child(memberId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(userSnapshot: DataSnapshot?) {
                    if (userSnapshot != null) {
                        val member = userSnapshot.getValue(User::class.java)
                        member?.id = userSnapshot.key
                        if (member != null) {
                            membersToDisplay.add(member)
                            memberCount--
                            if (memberCount == 0) {
                                //Display list
                                invitedMemberList.visibility = View.VISIBLE
                                val adapter = MemberListAdapter(this@CreateGroupActivity, membersToDisplay, true, invitedMemberIds)
                                invitedMemberList.adapter = adapter
                            }
                        }
                    }
                }
                override fun onCancelled(p0: DatabaseError?) {}
            })
        }
    }

    private fun getDescriptions() {
        groupName = groupNameEditText.text.toString()
        groupPrefs = groupPrefs_editText.text.toString()
        extraInfo = extraInfo_editText.text.toString()
    }

    private fun showHelp(title: Int, text: Int, butt_text: Int) {
        val alert = AlertDialog.Builder(this)
                .setMessage(resources.getString(text))
                .setTitle(resources.getString(title))
                .setCancelable(false)
                .setPositiveButton(resources.getString(butt_text), null)
        alert.show()
    }

    private fun checkFields(): Boolean {
        val builder = AlertDialog.Builder(this)
        builder.setPositiveButton(R.string.okay, null)

        if (chosenDestId == Group.DESTINATION_DEFAULT && groupImageUrl.isEmpty()) {
            builder.setMessage(R.string.dest_field_incomplete)
                    .show()
            return false
        } else if (date.startYear == 0) {
            builder.setMessage(R.string.date_start_field_incomplete)
                    .show()
            return false
        } else if (date.endYear == 0) {
            builder.setMessage(R.string.date_end_field_incomplete)
                    .show()
            return false
        } else if (date.getStartDate() > date.getEndDate()) {
            builder.setMessage(R.string.date_invalid)
                    .show()
            return false
        } else if (date.getStartDate() == date.getEndDate() && date.startTime >= date.endTime) {
            builder.setMessage("Please fix the start and end times.") //TODO string resources
                    .show()
            return false
        } else if (experienceAns == EXPERIENCE_ANS_DEFAULT) {
            builder.setMessage(R.string.exp_field_incomplete)
                    .show()
            return false
        } else if (groupPrefs.length < G_PREFS_CHAR_MIN || groupPrefs.length > G_PREFS_CHAR_MAX
                || extraInfo.length < E_INFO_CHAR_MIN || extraInfo.length > E_INFO_CHAR_MAX) {
            builder.setMessage(R.string.text_field_incomplete)
                    .show()
            return false
        } else if (date.getStartDate() < General.getDateLong(TimeManager.globalTimeStamp) || date.getEndDate() < General.getDateLong(TimeManager.globalTimeStamp)) {
            builder.setMessage(R.string.date_past_invalid)
                    .show()
            return false
        } else {
            return true
        }
    }
}