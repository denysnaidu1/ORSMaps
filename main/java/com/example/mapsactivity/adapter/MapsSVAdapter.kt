package com.example.mapsactivity.adapter

import android.content.Context
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult
import com.example.mapsactivity.LocationItem
import com.example.mapsactivity.MainActivity
import com.example.mapsactivity.R
import kotlinx.android.parcel.Parcelize

class MapsSVAdapter(parent: MainActivity, data: List<GeocodeResult>) : BaseAdapter() {
    private val mContext=parent.applicationContext
    private var listData : ArrayList<LocationItem> = ArrayList()
    private val inflater=LayoutInflater.from(mContext)
    private val caller=parent
    private var geocode : List<GeocodeResult> = ArrayList()
    init {
        for(i in data){
            val temp=LocationItem("Address:",i.label)
            listData.add(temp)
        }
        geocode=data
    }

    fun destroy(){
        listData.clear()
    }

    override fun getCount(): Int {
        return listData.size
    }

    override fun getItem(position: Int): Any {
        return listData[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var holder = ViewHolder()
        var view = convertView
        if(convertView==null){
            view = inflater.inflate(R.layout.list_svaddress_items,null)
            holder.address=view.findViewById(R.id.tv_listAddress)
            holder.details=view.findViewById(R.id.tv_detailAddress)
            view.tag=holder
        }else{
            holder = view?.tag as ViewHolder
        }
        holder.address!!.text=listData[position].address
        holder.details!!.text=listData[position].details

        view!!.setOnClickListener {
            caller.displaySearchResult(geocode[position])
        }
        return view
    }

    inner class ViewHolder {
        var address:TextView?=null
        var details:TextView?=null
    }

}

