package com.explorify.xplore.xplore

import android.app.Activity
import android.app.Fragment
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

import java.util.ArrayList

import kotlinx.android.synthetic.main.list_item_reserve.view.*
import kotlinx.android.synthetic.main.search_layout2.resultsRV
import kotlinx.android.synthetic.main.search_layout2.searchEditText

/**
 * Created by Nika on 11/9/2016.
 */

class LibraryFragment : Fragment(), TextView.OnEditorActionListener {

    private var resultIDs: List<Int>? = ArrayList()
    private val answerCards = ArrayList<ReserveCard>()
    private var reserveCards = ArrayList<ReserveCard>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.search_layout2, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Setting layoutmanager
        resultsRV.layoutManager = LinearLayoutManager(activity)

        val dbManager = DBManager(activity)
        dbManager.openDataBase()

        //setting up searchbar
        searchEditText.setSingleLine(true)
        searchEditText.setHint(R.string.search_hint)
        searchEditText.setSelectAllOnFocus(true)
        searchEditText.setOnEditorActionListener(this)

        //Clear answer list
        answerCards.clear()

        //Load all reserveCards from DB
        reserveCards = dbManager.getAllReserveCards()
        displayResults(reserveCards)
    }

    fun displayResults(results: List<ReserveCard>) {
        //Setting adapter
        resultsRV.adapter = RVadapter(results, activity)
    }

    class RVadapter(val results: List<ReserveCard>, val activity: Activity) : RecyclerView.Adapter<RVadapter.ResultViewHolder>(){

        class ResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
            internal val reserveName: TextView
            internal val reserveImage: ImageView
            internal val reserveIcon: ImageView

            init{
                reserveName = itemView.reserveNameTextView
                reserveImage = itemView.reserveImageView
                reserveIcon = itemView.reserveIconImageView
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder =
                ResultViewHolder(
                        LayoutInflater.from(parent.context)
                                .inflate(R.layout.list_item_reserve, parent, false)
                )

        override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
            holder.reserveName.setText(results[position].name)
            holder.reserveImage.setImageResource(results[position].imageId)
            holder.itemView.setOnClickListener {
                General.HideKeyboard(activity)
                General.openReserveInfoFragment(results[position].id, activity)
            }
        }

        override fun onAttachedToRecyclerView(recyclerView: RecyclerView?) {
            super.onAttachedToRecyclerView(recyclerView)
        }

        override fun getItemCount(): Int = results.size
    }

    //TODO change this in dbManager to just return all cards instead of the loop here
    private fun populateCardList(reserveCards: ArrayList<ReserveCard>, dbManager: DBManager) {
        val table = General.DB_TABLE
        reserveCards.clear()

        Thread(Runnable {
            //Getting each resID separately
            for (i in 0..MainActivity.RESERVE_NUM - 1) {
                reserveCards.add(dbManager.getReserveCard(i))
                resultsRV.post { resultsRV.adapter.notifyDataSetChanged() }
            }
        }).start()

    }

    override fun onEditorAction(textView: TextView, i: Int, keyEvent: KeyEvent): Boolean {
        //TODO this is a hack, change this
        val dbManager = DBManager(activity)
        dbManager.openDataBase()
        searchListItems(textView.text.toString().toLowerCase(), dbManager)
        return false
    }

    //Searches DB for query
    private fun searchListItems(query: String, dbManager: DBManager) {
        answerCards.clear()

        //Searching Database
        resultIDs = dbManager.getIdFromQuery(query, General.DB_TABLE)

        //Returning Results
        if (resultIDs == null) {
            Toast.makeText(activity, R.string.search_no_results, Toast.LENGTH_SHORT).show()
        } else {
            var index = 0
            for (result in resultIDs!!) {
                //result is the single ID of an answer
                answerCards.add(index, reserveCards[result])
                index++
            }

            //Setting adapter
            resultsRV.adapter = RVadapter(answerCards, activity)
        }
    }
}
