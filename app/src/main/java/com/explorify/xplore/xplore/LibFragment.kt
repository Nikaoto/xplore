package com.explorify.xplore.xplore

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity

import kotlinx.android.synthetic.main.lib_element_layout.*

/**
 * Created by nikao on 11/16/2016.
 */

class LibFragment() : FragmentActivity() {

    internal val mActivity = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.lib_element_layout)

        //Sets up Layout acording to info from chosen Reserve
        setupLayout(LoadReserve(intent.getIntExtra("chosen_element", 0)))
    }

    override fun onResume() {
        if (ThirdFragment.MAPS_CLOSED) {
            fragmentManager.popBackStack()
        }
        super.onResume()
    }

    fun setupLayout(reserve: Reserve) {
        headerButton.background = reserve.drawable
        headerButton.text = reserve.name
        descriptionTextView.text = reserve.description
        faunaTextView.text = reserve.fauna
        floraTextView.text = reserve.flora
        equipmentTextView.text = reserve.equipment
        tagsTextView.text = reserve.extratags
        difficultyRatingBar.rating = reserve.difficulty.toFloat()

        showonmapButton.setOnClickListener() {
            ThirdFragment.MAPS_CLOSED = false
            val intent = Intent(mActivity, MapsActivity::class.java)
            intent.putExtra("show_reserve", true)
            intent.putExtra("reserve_name", reserve.name)
            intent.putExtra("reserve_latitude", reserve.location.latitude)
            intent.putExtra("reserve_longitude", reserve.location.longitude)
            mActivity.startActivity(intent)
        }
    }

    //Loads reserve from database by Id and returns it
    private fun LoadReserve(resId: Int): Reserve {
        val dbManager = DBManager(this)
        dbManager.openDataBase()
        //Getting reserve info from DB
        return dbManager.getReserve(resId)
    }
}
