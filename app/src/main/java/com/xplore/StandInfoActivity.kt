package com.xplore

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v4.view.ViewCompat
import android.view.View
import android.widget.TextView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import com.xplore.base.BaseActivity
import com.xplore.reserve.Icons
import com.xplore.util.FirebaseUtil
import kotlinx.android.synthetic.main.stand_info.*

/**
 * Created by Nika on 9/28/2017.
 *
 * Info about a certain stand
 *
 */

class StandInfoActivity : BaseActivity(), AppBarLayout.OnOffsetChangedListener {

    private val IMAGE_SHOW_PERCENT = 100
    private var maxScrollSize = 0
    private var isImageHidden = false


    companion object {
        const val ARG_CHOSEN_STAND_ID = "chosen_stand"

        @JvmStatic
        fun getStartIntent(context: Context, standId: String): Intent
                = Intent(context, StandInfoActivity::class.java).putExtra(ARG_CHOSEN_STAND_ID, standId)
    }

    val standId: String by lazy {
        intent.getStringExtra(ARG_CHOSEN_STAND_ID)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.stand_info)

        // Set up menu
        toolbar.setNavigationOnClickListener { onBackPressed() }
        appBar.addOnOffsetChangedListener(this)

        // Start loading data
        FirebaseUtil.getStandRef(standId).addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot?) {
                        if (dataSnapshot != null) {
                            val stand = dataSnapshot.getValue(Stand::class.java)
                            if (stand != null) {
                                displayData(stand)
                            } else {
                                finish()
                            }
                        } else {
                            finish()
                        }
                    }

                    override fun onCancelled(p0: DatabaseError?) { }
                }
        )
    }

    override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
        if(maxScrollSize == 0)
            maxScrollSize = appBarLayout.totalScrollRange

        val currentScrollPercentage = (Math.abs(verticalOffset)) * 100 / maxScrollSize

        if(currentScrollPercentage >= IMAGE_SHOW_PERCENT)
            if(!isImageHidden){
                isImageHidden = true
                ViewCompat.animate(reserveIconFAB).scaleY(0f).scaleX(0f).start()
            }
        if(currentScrollPercentage < IMAGE_SHOW_PERCENT)
            if(isImageHidden){
                isImageHidden = false
                ViewCompat.animate(reserveIconFAB).scaleY(1f).scaleX(1f).start()
            }
    }

    private fun TextView.safeSetText(txt: String?) {
        if (txt != null && txt.isNotEmpty()) {
            this.text = txt
        } else {
            ((this.parent as View).parent as View).visibility = View.GONE
        }
    }

    private fun displayData(stand: Stand) {
        // Name
        collapsingToolbar.title = stand.id

        // Image
        if (stand.banner_image_url.isNotEmpty()) {
            Picasso.with(this)
                    .load(stand.banner_image_url)
                    .into(standImageView)
        } else {
            Picasso.with(this)
                    .load(stand.image_url)
                    .into(standImageView)
        }

        // Type FAB
        reserveIconFAB.setImageResource(Icons.iliauni)

        // Description
        descriptionTextView.safeSetText(stand.description)

        // Show On Map
        if (stand.hasNoLocation()) {
            showonmapButton.visibility = View.GONE
        } else {
            showonmapButton.setOnClickListener {

                /*startActivity(GroupMapActivity.getStartIntent(
                                this,
                                true,
                                reserve.name,
                                reserve.location.latitude,
                                reserve.location.longitude
                        )
                )*/
            }
        }
    }
}