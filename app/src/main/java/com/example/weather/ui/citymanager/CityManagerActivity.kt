package com.example.weather.ui.citymanager

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.support.v7.widget.helper.ItemTouchHelper
import android.text.TextUtils
import com.chad.library.adapter.base.callback.ItemDragAndSwipeCallback
import com.chad.library.adapter.base.listener.OnItemDragListener
import com.chad.library.adapter.base.listener.OnItemSwipeListener
import com.example.weather.R
import com.example.weather.base.BaseActivity
import com.example.weather.other.db.CityWeather
import com.example.weather.ui.adapter.CityManagerAdapter
import com.example.weather.ui.main.MainActivity
import com.example.weather.util.LogUtil
import com.example.weather.util.initToolbar
import com.lljjcoder.style.citylist.CityListSelectActivity
import com.lljjcoder.style.citylist.bean.CityInfoBean
import kotlinx.android.synthetic.main.activity_city_manager.*
import org.jetbrains.anko.toast
import org.litepal.crud.DataSupport

/**
 *
 */
class CityManagerActivity : BaseActivity() {

    override fun getLayoutId(): Int {
        return R.layout.activity_city_manager
    }

    private lateinit var mAdapter: CityManagerAdapter
    private lateinit var list: List<CityWeather>
    private var dataChanged = false
    private var selectedPosition = -1
//    private val ADD_ITEM = 1

    override fun initView(savedInstanceState: Bundle?) {
        initToolbar(toolbar as Toolbar)
//        val item=DataSupport.findFirst(CityWeather::class.java)

        fab.setOnClickListener {
            val intent = Intent(this@CityManagerActivity, CityListSelectActivity::class.java)
            startActivityForResult(intent, CityListSelectActivity.CITY_SELECT_RESULT_FRAG)
//            val intent = Intent(this@CityManagerActivity, ChooseActivity::class.java)
//            startActivityForResult(intent, ADD_ITEM)
        }
        initViewPager()
    }


    private fun initViewPager(){
        list = DataSupport.order("countyId").find(CityWeather::class.java)
        if (list.isEmpty())
            return
        mAdapter = CityManagerAdapter(list)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@CityManagerActivity)
            adapter = mAdapter
        }
        mAdapter.apply {
            //设置第一个item禁止滑动和拖拽
            val itemDragAndSwipeCallback = object : ItemDragAndSwipeCallback(mAdapter) {
                override fun onMove(recyclerView: RecyclerView?, source: RecyclerView.ViewHolder?, target: RecyclerView.ViewHolder?): Boolean =
                        if (source?.adapterPosition == 0 || target?.adapterPosition == 0)
                            false
                        else
                            super.onMove(recyclerView, source, target)

                override fun getMovementFlags(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?): Int {
                    return if (viewHolder?.adapterPosition == 0)
                        makeMovementFlags(0, 0)
                    else
                        super.getMovementFlags(recyclerView, viewHolder)
                }

            }
            val itemTouchHelper = ItemTouchHelper(itemDragAndSwipeCallback)
            itemTouchHelper.attachToRecyclerView(recyclerView)
            itemDragAndSwipeCallback.setSwipeMoveFlags(ItemTouchHelper.START or ItemTouchHelper.END)


            var draStartPosition = 0 //起始拖拽的位置
            // 开启拖拽   (获得当前有效行为，用dataChanged变量)
            enableDragItem(itemTouchHelper, R.id.root, true)
            setOnItemDragListener(object : OnItemDragListener {
                override fun onItemDragMoving(source: RecyclerView.ViewHolder?, from: Int, target: RecyclerView.ViewHolder?, to: Int) {
                }

                override fun onItemDragStart(viewHolder: RecyclerView.ViewHolder?, pos: Int) {
                    draStartPosition = pos
                }

                override fun onItemDragEnd(viewHolder: RecyclerView.ViewHolder?, pos: Int) {
                    if (draStartPosition != pos) {
                        dataChanged = true
                    }
                }

            })

            //思路: 删除的时候，1 数据库的更新，2 mAdapter的更新  3 dataChanged =true (用于通知MainActivity更新)
            // 开启滑动
            mAdapter.enableSwipeItem()
            setOnItemSwipeListener(object : OnItemSwipeListener {
                //1 删除数据库中的数据 2 弹出提示  3 更新adapter
                override fun onItemSwiped(viewHolder: RecyclerView.ViewHolder?, pos: Int) {
//                    val deleteItem = mAdapter.data[pos]
//                    val deletePos=pos
//                    Snackbar.make(fab, "删除成功！", Snackbar.LENGTH_LONG)
//                            .apply {
////                            数据库保存失败
//                                setAction("撤销") {
////                                    CityWeather(deleteItem.countyName,deletePos)
////                                            .save()
//                                    deleteItem.save() //重新保存
//                                    mAdapter.addData(deletePos, deleteItem)
////                                    mAdapter.notifyItemInserted(deletePos)
//                                }
//                            }
//                            .show()
                    toast("删除成功！")
                    //原来数据库的删除操作需要放在mAdapter之前，不然会报错IndexOutException
                    DataSupport.deleteAll(CityWeather::class.java, "countyId = ?", mAdapter.data[pos].countyId.toString())
                    mAdapter.apply {
                        data.removeAt(pos)
                        notifyItemRemoved(pos)
                    }
                    dataChanged = true
                }

                override fun onItemSwipeStart(viewHolder: RecyclerView.ViewHolder?, pos: Int) {

                }

                override fun clearView(viewHolder: RecyclerView.ViewHolder?, pos: Int) {

                }

                override fun onItemSwipeMoving(canvas: Canvas?, viewHolder: RecyclerView.ViewHolder?, dX: Float, dY: Float, isCurrentlyActive: Boolean) {

                }
            })

            setOnItemClickListener { _, _, position ->
                selectedPosition = position
                val intent = Intent().apply {
                    putExtra(MainActivity.SELECTED_ITEM, selectedPosition)
                }
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }
    }

    private fun addData(countyName: String) {
        CityWeather(countyName, mAdapter.data.size)
                .apply {
                    save()
                    mAdapter.addData(this)
                    mAdapter.notifyDataSetChanged()
                    dataChanged = true
                }
    }

    /**
     *todo: 后期可以优化一下
     */
    override fun onBackPressed() {
        for (i in 0 until mAdapter.data.size) {
            val values = ContentValues()
            values.put("countyId", i)
            DataSupport.updateAll(CityWeather::class.java, values, "countyName = ?", mAdapter.data[i].countyName)
        }

        val intent = Intent().apply {
            putExtra(MainActivity.CHANGE, dataChanged)
            putExtra(MainActivity.SELECTED_ITEM, selectedPosition)
        }
        setResult(Activity.RESULT_OK, intent)
        super.onBackPressed()
    }

    //todo: 一开始以为是RxBus导致的闪退，所以使用startActivityForResult代替，后续可以修改
    //mAdapter的更新需要放在当前Activity/Fragment的处于用户可操作状态才能进行,否则会崩溃(crash)
    //解决办法是放在onBackPress()方法中去通知上一个Activity处理更新操作
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            CityListSelectActivity.CITY_SELECT_RESULT_FRAG -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        if (data == null)
                            return
                        val bundle = data.extras
                        val cityInfoBean = bundle.getParcelable<CityInfoBean>("cityinfo")
                        when {
                            cityInfoBean == null -> return
                            TextUtils.isEmpty(cityInfoBean.name) -> {
                                toast("暂无该城市天气信息")
                                return
                            }
                            else -> {
                                mAdapter.data.forEach {
                                    if (it.countyName == cityInfoBean.name) {
                                        toast("重复的城市!")
                                        return
                                    }
                                }
                            }
                        }

                        LogUtil.d("CityManagerActivity", "cityName::::::${cityInfoBean.name}")
                        addData(cityInfoBean.name)
                    }

                }
            }
            else -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
//                val countyName = data?.getStringExtra("item")
//                if (!TextUtils.isEmpty(countyName)) {
//                    addData(countyName!!)
//                }

                    }

                }
            }
        }
    }

    companion object {

        fun start(context: Context) {
            val intent = Intent(context, CityManagerActivity::class.java)
            context.startActivity(intent)
        }
    }
}
