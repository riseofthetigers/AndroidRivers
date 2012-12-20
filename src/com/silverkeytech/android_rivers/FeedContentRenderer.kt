/*
Android Rivers is an app to read and discover news using RiverJs, RSS and OPML format.
Copyright (C) 2012 Dody Gunawinata (dodyg@silverkeytech.com)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>
*/

package com.silverkeytech.android_rivers

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Handler
import android.os.Message
import android.os.Messenger
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import com.silverkeytech.android_rivers.riverjs.FeedItemMeta
import go.goyalla.dict.arabicDictionary.file.ArabicReshape
import com.silverkeytech.android_rivers.outliner.transformFeedOpmlToOpml
import com.silverkeytech.android_rivers.outliner.startOutlinerActivity
import com.silverkeytech.android_rivers.outliner.traverse
import com.silverkeytech.android_rivers.syndication.FeedItem

//Manage the rendering of each news item in the river list
public class FeedContentRenderer(val context: Activity, val language: String){
    class object {
        val STANDARD_NEWS_COLOR = android.graphics.Color.GRAY
        val STANDARD_NEWS_IMAGE = android.graphics.Color.CYAN
        val STANDARD_NEWS_PODCAST = android.graphics.Color.MAGENTA
        val STANDARD_NEWS_SOURCE = android.graphics.Color.BLUE

        public val TAG: String = javaClass<FeedContentRenderer>().getSimpleName()
    }

    //hold the view data for the list
    public data class ViewHolder (var news: TextView, val indicator: TextView)

    //show and prepare the interaction for each individual news item
    fun handleNewsListing(feedItems: List<FeedItem>) {
        val textSize = context.getVisualPref().getListTextSize()

        //now sort it so people always have the latest news first

        var list = context.findView<ListView>(android.R.id.list)

        var inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        var adapter = object : ArrayAdapter<FeedItem>(context, R.layout.news_item, feedItems) {
            public override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
                var currentView = convertView
                var holder: ViewHolder?

                var currentNewsItem = feedItems[position]
                var news = currentNewsItem.toString().trim()

                if (currentView == null){
                    currentView = inflater.inflate(R.layout.news_item, parent, false)

                    holder = ViewHolder(currentView!!.findViewById(R.id.news_item_text_tv) as TextView,
                            currentView!!.findViewById(R.id.news_item_indicator_tv) as TextView)

                    currentView!!.setTag(holder)
                }else{
                    holder = currentView?.getTag() as ViewHolder
                    Log.d(TAG, "List View reused")
                }

                handleText(holder!!.news, news, textSize.toFloat())

                if (news.isNullOrEmpty()){
                    currentView?.setVisibility(View.GONE)
                }   else{
                    currentView?.setVisibility(View.VISIBLE)
                }
                return currentView
            }
        }

        list.setOnItemClickListener(object : OnItemClickListener{
            public override fun onItemClick(p0: AdapterView<out Adapter?>?, p1: View?, p2: Int, p3: Long) {
                val currentNews = feedItems.get(p2);

                var dialog = AlertDialog.Builder(context)

                var dlg: View = inflater.inflate(R.layout.news_details, null)!!
                var msg: String

                if (currentNews.description.isNullOrEmpty() && !currentNews.title.isNullOrEmpty())
                    msg = scrubHtml(currentNews.title)
                else
                    msg = scrubHtml(currentNews.description)

                //take care of color
                var theme = context.getVisualPref().getTheme()
                if (theme == R.style.Theme_Sherlock_Light_DarkActionBar)
                    dlg.setBackgroundColor(android.graphics.Color.WHITE)
                else if (theme == R.style.Theme_Sherlock)
                    dlg.setBackgroundColor(android.graphics.Color.BLACK)

                var body = dlg.findViewById(R.id.news_details_text_tv)!! as TextView
                handleText(body, msg, textSize.toFloat())
                handleTextColor(context, body)

                var source = dlg.findViewById(R.id.news_details_source_tv)!! as TextView

                dialog.setView(dlg)

                var createdDialog = dialog.create()!!
                createdDialog.setCanceledOnTouchOutside(true)
                dlg.setOnClickListener {
                    createdDialog.dismiss()
                }

                createdDialog.show()
            }
        })

        list.setAdapter(adapter)
    }

    val arabicFont = Typeface.createFromAsset(context.getAssets(), "DroidKufi-Regular.ttf")

    fun handleText(text: TextView, content: String, textSize: Float) {
        when(language){
            "ar" -> {
                Log.d(TAG, "Switching to Arabic Font")
                text.setTypeface(arabicFont)
                text.setText(ArabicReshape.reshape(content))
                text.setGravity(Gravity.RIGHT)
                text.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize - 3.toFloat())
            }
            else -> {
                text.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
                text.setText(content);
            }
        }
    }

    fun handleTextColor(context: Activity, text: TextView) {
        var theme = context.getVisualPref().getTheme()
        if (theme == R.style.Theme_Sherlock_Light_DarkActionBar)
            text.setTextColor(android.graphics.Color.BLACK)
        else if (theme == R.style.Theme_Sherlock)
            text.setTextColor(android.graphics.Color.WHITE)
    }
}