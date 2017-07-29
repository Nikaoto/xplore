package com.xplore.groups.search

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.Toast
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import com.xplore.*
import com.xplore.R
import com.xplore.database.DBManager
import com.xplore.groups.Group
import com.xplore.reserve.Icons
import com.xplore.reserve.ReserveInfoActivity
import com.xplore.user.User

import java.util.ArrayList

import kotlinx.android.synthetic.main.group_info.*
import kotlinx.android.synthetic.main.reserve_list_item.*

/**
* Created by Nikaoto on 2/12/2017.
* TODO write description of this class - what it does and why.
*/

class GroupInfoActivity : Activity() {

    //Firebase
    private val FIREBASE_TAG_MEMBER_IDS = "member_ids"
    private val FIREBASE_TAG_GROUP_IDS = "group_ids"
    private val DBref = FirebaseDatabase.getInstance().reference
    private val firebaseGroupsRef = DBref.child("groups")
    private val firebaseUsersRef = DBref.child("users")

    private var groupId = ""
    private var reserveID = 0 //TODO remove this and load submitted image or map image instead
    private var memberCount = 1
    private val members = ArrayList<User>()

    //The variables which contain the current group/member info
    private var currentGroup = Group()
    private var tempMember = User()

    companion object {
        @JvmStatic
        fun getStartIntent(context: Context, groupId: String, reserveId: Int)
                = Intent(context, GroupInfoActivity::class.java)
                        .putExtra("group_id", groupId)
                        .putExtra("reserve_id", reserveId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.group_info)
        //buildUserBase();

        //Receives group data from last intent
        val intent = this.intent
        groupId = intent.getStringExtra("group_id")
        reserveID = intent.getIntExtra("reserve_id", 0)

        initMemberList()
        loadGroupData(groupId)
        applyReserveData()
    }

    /* Only runs when a member from the current group is viewing it.
        Adds buttons like "leave group" or "invite members" */
    private fun configureGroupLayoutForMember() {
        leaveGroupButton.visibility = View.VISIBLE
        leaveGroupButton.setOnClickListener {
            popLeaveGroupConfirmationDialog()
        }
    }

    /* Runs when the current user is viewing a group he's not in */
    private fun configureGroupLayoutForOutsider() {
        //TODO do stuff here
    }

    /* Runs when the current user is viewing the group is the leader */
    private fun configureGroupLayoutForLeader() {
        //TODO do stuff here
    }

    //Gets reserve data from local database and displays it on the reserve card
    private fun applyReserveData() {
        val dbManager = DBManager(this)
        dbManager.openDataBase()
        val tempReserveCard = dbManager.getReserveCard(reserveID)

        reserveCardView.setOnClickListener {
            startActivity(ReserveInfoActivity.getStartIntent(this, reserveID))
        }
        reserveNameTextView.text = tempReserveCard.name
        groupImageView.setImageResource(tempReserveCard.imageId)
        reserveIconImageView.setImageResource(Icons.grey[tempReserveCard.iconId])
    }

    private fun initMemberList() {
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        membersRecyclerView.setHasFixedSize(true)
        membersRecyclerView.layoutManager = layoutManager
    }

    /*
    //IN CASE WE'RE USING A SECOND FIREBASE DATABASE FOR USERS
    private void buildUserBase()
    {
    USERBASE_KEY = getResources().getString(R.string.user_firebase_key);
    USERBASE_APPID = getResources().getString(R.string.firebase_appid);
    USERBASE_URL = getResources().getString(R.string.user_firebase_url);
    userBaseOptions = new FirebaseOptions.Builder()
    .setApiKey(USERBASE_KEY)
    .setApplicationId(USERBASE_APPID)
    .setDatabaseUrl(USERBASE_URL)
    .build();
    try {
    if (FirebaseApp.getApps(getActivity()).get(1).equals(null)) {
    FirebaseApp.initializeApp(getActivity(), userBaseOptions, "userbase");
    } catch (IndexOutOfBoundsException e) {
    FirebaseApp.initializeApp(getActivity(), userBaseOptions, "userbase");
    }
    userBaseApp = FirebaseApp.getInstance("userbase");
    userDB = FirebaseDatabase.getInstance(userBaseApp);
    }
    */

    private fun loadGroupData(groupId: String) {
        firebaseGroupsRef.child(groupId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot?) {
                //Checking if group exists
                if (dataSnapshot != null) {
                    //creating the temporary (current) group
                    currentGroup = dataSnapshot.getValue(Group::class.java)!!
                    currentGroup.setGroup_id(dataSnapshot.key)
                    memberCount = currentGroup.getMember_ids().size

                    if (currentGroup.getMember_ids()[0] == General.currentUserId) {
                        configureGroupLayoutForLeader()
                    } else {
                        //Checking if current user is a member and configuring layout accordingly
                        if (currentGroup.getMember_ids().contains(General.currentUserId)) {
                            configureGroupLayoutForMember()
                        } else {
                            configureGroupLayoutForOutsider()
                        }
                    }

                    for (memberId in currentGroup.getMember_ids()) {
                        if (memberId != null) {
                            getUserInfo(memberId)
                        } else {
                            memberCount--
                            if (memberCount == 0) {
                                applyGroupData()
                            }
                        }
                    }
                } else {
                    //TODO string resources
                    Toast.makeText(applicationContext,
                            "The group does not exist",
                            Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    //Gets user info from Firebase using userId
    private fun getUserInfo(userId: String) {
        firebaseUsersRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot?) {
                //Checking if member exists
                if (dataSnapshot!!.exists()) {
                    tempMember = dataSnapshot.getValue(User::class.java)!! //Getting member info
                    tempMember.setId(userId) //Setting user Id
                    members.add(tempMember) //Setting member info
                    memberCount-- //Iterating member index
                    if (memberCount == 0) { //Checking if member list retrieval finished
                        applyGroupData()
                    }
                } else {
                    //TODO string resources
                    Toast.makeText(applicationContext,
                            "There was an error retreiving the members",
                            Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    //Displays the already-retrieved data of the group
    private fun applyGroupData() {
        groupProgressBar.visibility = View.GONE

        val leader = members[0];
        //Displaying leader
        //Profile picture
        Picasso.with(this).invalidate(leader.getProfile_picture_url())
        Picasso.with(this)
                .load(leader.getProfile_picture_url())
                .transform(CircleTransformation(leaderImageView.width, leaderImageView.height))
                .into(leaderImageView)
        //Name
        leaderFnameTextView.text = leader.getFname()
        leaderLnameTextView.text = leader.getLname()
        //Age
        val age = General.calculateAge(TimeManager.globalTimeStamp, leader.getBirth_date())
        leaderAgeTextView.text = "${getString(R.string.age)}: $age"
        //Telephone
        leaderTelTextView.text = "${getString(R.string.tel)}: ${leader.getTel_num()}"
        //Reputation
        leaderRepTextView.text = leader.getReputation().toString()

        //Setting experienced icon
        if (currentGroup.isExperienced) {
            groupExpImageView.setImageResource(R.drawable.ic_check)
        } else {
            groupExpImageView.setImageResource(R.drawable.ic_x)
        }

        groupExpImageView.setOnClickListener {
            popExperienceInfoDialog()
        }

        startDateTextView.text = General.putSlashesInDate(currentGroup.getStart_date())
        endDateTextView.text = General.putSlashesInDate(currentGroup.getEnd_date())

        groupPrefsTextView.text = currentGroup.getGroup_preferences()
        groupExtraInfoTextView.text = currentGroup.getExtra_info()

        //Displaying members
        val adapter = MemberListAdapter(this, members)
        membersRecyclerView.adapter = adapter
    }

    private fun popLeaveGroupConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        //TODO string resources
        builder.setTitle("Leave Group?")
                .setMessage("Are you sure you want to leave this group?")
                .setPositiveButton("Yes", { _, _: Int -> leaveGroup() })
                .setNegativeButton("No", null)
        builder.show()
    }

    private fun leaveGroup() {
        firebaseGroupsRef.child("$groupId/$FIREBASE_TAG_MEMBER_IDS")
                .orderByValue().equalTo(General.currentUserId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot?) {
                        if (dataSnapshot != null) {
                            for (itemSnapshot in dataSnapshot.children) {
                                removeUserFromGroup(itemSnapshot.ref)
                            }

                        } else {
                            //TODO string resources
                            Toast.makeText(this@GroupInfoActivity,
                                    "Server error: couldn't leave group. Please try again later",
                                    Toast.LENGTH_SHORT).show()
                        }
                        finish() //TODO recreate with the same parameters
                    }

                    override fun onCancelled(p0: DatabaseError?) {}
                })
    }

    private fun removeUserFromGroup(dbRef: DatabaseReference) {
        firebaseUsersRef.child("${General.currentUserId}/$FIREBASE_TAG_GROUP_IDS")
                .orderByValue().equalTo(groupId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot?) {
                        if (dataSnapshot != null) {
                            dbRef.removeValue()
                            // Don't move ^this^ up to leaveGroup(), deletions need to be as close
                            // as possible in case of sudden disconnection
                            for (itemSnapshot in dataSnapshot.children) {
                                itemSnapshot.ref.removeValue()
                            }
                            //TODO resort member ids
                            //TODO string resources
                            Toast.makeText(this@GroupInfoActivity,
                                    "You have left the group",
                                    Toast.LENGTH_SHORT).show()
                        } else {
                            // If the user gets here, then... Well.. fuck...
                            // we'll need to fix the issue from the firebase console
                            //TODO string resources
                            Toast.makeText(this@GroupInfoActivity,
                                    "Server error: couldn't leave group. Please try again later",
                                    Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(p0: DatabaseError?) {}
                }
        )
    }

    //Displays information about the experience icon (X and tick)
    private fun popExperienceInfoDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.what_is_this)
                .setMessage(R.string.group_exp_help)
                .setPositiveButton(R.string.okay, null)
        builder.show()
    }
}
