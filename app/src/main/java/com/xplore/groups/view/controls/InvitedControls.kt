package com.xplore.groups.view.controls

import android.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.xplore.General
import com.xplore.R
import kotlinx.android.synthetic.main.invited_controls.*

/**
 * Created by Nika on 8/4/2017.
 *
 * აღწერა:
 * ეს ფრაგმენტი ჩნდება მხოლოდ მაშინ როდესაც მომხმარებელი უყურებს ჯგუფს, რომელშიც იგი დაპატიჟეს და
 * აძლევს არჩევანს (მიიღოს ან უარყოს მოწვევა).
 *
 * Description:
 * This fragment is only shown when the user is viewing a group that he's invited in. The user
 * can choose to accept or decline the invite from this fragment.
 *
 */

class InvitedControls : Fragment() {

    //Firebase
    private val FIREBASE_MEMBER_IDS_TAG  = "member_ids"
    private val FIREBASE_INVITED_MEMBER_IDS_TAG  = "invited_member_ids"
    private val currentUserRef = FirebaseDatabase.getInstance().reference
            .child("users").child(General.currentUserId)
    private val invitedGroupIdsRef = currentUserRef.child("invited_group_ids")
    private val joinedGroupIdsRef = currentUserRef.child("group_ids")
    private lateinit var currentGroupRef: DatabaseReference
    //
    private lateinit var groupId: String

    companion object {
        @JvmStatic
        fun newInstance(groupId: String): InvitedControls {
            val fragment = InvitedControls()
            val args = Bundle()
            args.putString("groupId", groupId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            = inflater.inflate(R.layout.invited_controls, container, false)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        groupId = arguments.getString("groupId")
        currentGroupRef = FirebaseDatabase.getInstance().reference.child("groups").child(groupId)

        acceptInviteButton.setOnClickListener {
            acceptInvite()
        }

        declineInviteButton.setOnClickListener {
            declineInvite()
        }
    }

    private fun acceptInvite() {
        //Add to joined groups
        joinedGroupIdsRef.child(groupId).setValue(false)
        //Remove from invited groups
        invitedGroupIdsRef.child(groupId).removeValue()
        //Remove user id from group's invited_member_ids
        currentGroupRef.child(FIREBASE_INVITED_MEMBER_IDS_TAG).child(General.currentUserId).removeValue()
        //Add user id to group's memberIds
        currentGroupRef.child(FIREBASE_MEMBER_IDS_TAG).child(General.currentUserId).setValue(false)

        refresh()
    }

    private fun declineInvite() {
        //Remove from invited groups
        invitedGroupIdsRef.child(groupId).removeValue()
        //Remove this user from group's invited users
        currentGroupRef.child("invited_member_ids").child(General.currentUserId).removeValue()

        refresh()
    }

    private fun refresh() {
        val intent = activity.intent
        activity.finish()
        startActivity(intent)
    }
}