package com.example.weather.ui.main

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.support.v7.app.ActionBarDrawerToggle
import android.view.Gravity
import android.widget.RemoteViews
import com.example.weather.R
import com.example.weather.base.BaseActivity
import com.example.weather.mvp.contract.MainContract
import com.example.weather.mvp.presenter.MainPresenter
import com.example.weather.network.LocationHelper
import com.example.weather.other.db.CityWeather
import com.example.weather.ui.AboutActivity
import com.example.weather.ui.citymanager.CityManagerActivity
import com.example.weather.ui.setting.SettingActivity
import com.example.weather.util.LogUtil
import com.example.weather.util.ShareUtils
import com.example.weather.util.StatusBarUtil
import com.example.weather.util.getShareMessage
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.toast

/**
 * ViewPager+Fragment
 */
class MainActivity : BaseActivity(), MainContract.View {

    companion object {
        const val CHANGE = "change" // const val相当于public final static //val 可见性为private final static，并且val 会生成方法getNormalObject() ，通过方法调用访问。
        const val SELECTED_ITEM = "selected_item"
    }

    override lateinit var presenter: MainContract.Presenter

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
//        EventBus.getDefault().unregister(this)

    }

    override fun getLayoutId(): Int {
        return R.layout.activity_main
    }

    override fun initView(savedInstanceState: Bundle?) {
//        EventBus.getDefault().register(this)

        //todo: 测试用，否则注释掉
//        startActivity(Intent(this,CityManagerActivity::class.java))
        initToolbar()
        initNavView()
        initViewPager()

        MainPresenter(this, this)
        //申请定位权限
        presenter.start()
        //MMP，activity里能调用成功，写在presenter中就无效
        LocationHelper.instance
                .locate {
                    presenter.start(it)
                }
    }

    private fun initViewPager() {
//        val alphaAnimation = AlphaAnimation(0f, 1f)
//                .apply {
//                    duration = 260
//                    setAnimationListener(object : Animation.AnimationListener {
//                        override fun onAnimationStart(animation: Animation) {
//                            window.setBackgroundDrawable(//getResources().getDrawable(R.drawable.window_frame_color));
//                                    ColorDrawable(Color.BLACK))
//                            //				WeatherNotificationService.startServiceWithNothing(MainActivity.this); //设置前台服务，保活机制
//                        }
//
//                        override fun onAnimationRepeat(animation: Animation) {}
//                        override fun onAnimationEnd(animation: Animation) {}
//                    })
//                }

        mAdapter = object : FragmentPagerAdapter(supportFragmentManager) {
            override fun getItem(position: Int): Fragment {
                return fragments[position]
            }

            override fun getCount(): Int {
                return fragments.size
            }

            override fun getPageTitle(position: Int): CharSequence {
                return titles[position]
            }
        }

        viewPager.apply {
            //            setAnimation(alphaAnimation)
            offscreenPageLimit = mAdapter.count//设置预加载的页数
            adapter = mAdapter
        }
        pageTitle.setViewPager(viewPager)
    }

    private fun initNavView() {
        navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_city_manager -> startActivityForResult(Intent(this@MainActivity, CityManagerActivity::class.java), 1)
                R.id.nav_settings -> startActivity(Intent(this@MainActivity, SettingActivity::class.java))
                R.id.nav_about -> startActivity(Intent(this@MainActivity, AboutActivity::class.java))
            }
            drawer_layout.closeDrawer(Gravity.START)
            true
        }
    }

    private fun initToolbar() {
        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        StatusBarUtil.setPaddingSmart(this, pageTitle)

        toolbar.apply {
            inflateMenu(R.menu.menu_main) //使用这个方法的前提条件:不能使用setSupportActionBar(toolbar); getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_share -> {
                        LogUtil.d("MainActivity", "点击了toolbar")
                        (fragments[viewPager.currentItem] as WeatherFragment).shareData?.let {
                            LogUtil.d("MainActivity", "shareData::::$it")
                            ShareUtils.shareText(this@MainActivity, getShareMessage(it), "分享到")
                        }
                        true
                    }
                    else -> false
                }

            }

        }
//        setSupportActionBar(toolbar)
//        supportActionBar?.title=""
    }

//    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//        menuInflater.inflate(R.menu.menu_main,menu)
//        return true
//    }
//    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
//        when (item?.itemId) {
//            R.id.menu_share -> {
////                        ShareUtils.shareText(this@MainActivity, getString(R.string.share_message), "分享到")
//                (fragments[viewPager.currentItem] as WeatherFragment).shareData?.let {
//                    getShareMessage(it)
//                }
//                return true
//            }
//            else -> return super.onOptionsItemSelected(item)
//        }
//    }

    private val fragments by lazy { arrayListOf<Fragment>() }
    private val titles by lazy { arrayListOf<String>() }
    private lateinit var mAdapter: FragmentPagerAdapter
//    val currentId: Int=viewPager.currentItem

    //因为该方法会重复调用，记得将其中的所有list集合清空
    override fun initFragment(list: MutableList<CityWeather>, selectedPosition: Int, isRefresh: Boolean) {
        //TODO: 测试用
//        if (list.size <= 1 && list.size > 0) {
//            list.add(list[0])
//            CityWeather("嵊州市")
//              .save()
//        }

        if (fragments.size > 0) fragments.clear()
        for ((i, item) in list.withIndex()) {
            fragments.add(WeatherFragment.newInstance(item, i))
        }
        mAdapter.notifyDataSetChanged()

        if (titles.size > 0) titles.clear()
        list.forEach { titles.add(it.countyName) }
        viewPager.offscreenPageLimit = mAdapter.count
        pageTitle.notifyDataSetChanged()
        if (selectedPosition > -1) {
            startFragment(selectedPosition)
        }
    }

    override fun startFragment(selectedPosition: Int) {
        viewPager.currentItem = selectedPosition
//        pageTitle.notifyDataSetChanged()
    }


    override fun showThemeChange() {
        recreate()
//        initTheme()
    }

    override fun showErrorMessage(message: String) {
        toast(message)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (resultCode) {
            Activity.RESULT_OK -> {

                val boolean = data.getBooleanExtra(CHANGE, false)
                val selectedItem = data.getIntExtra(SELECTED_ITEM, -1)
                presenter.refresh(selectedItem, boolean)
            }
        }
    }

    fun updateAppWidget() {
        //1 First, get the App Widget ID from the Intent that launched the Activity:
        val extras = intent.extras
        if (extras != null) {
            val mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID)
            //2  Perform your App Widget configuration.
            //3 When the configuration is complete, get an instance of the AppWidgetManager by calling getInstance(Context):
            val appWidManager = AppWidgetManager.getInstance(this)
            //4 Update the App Widget with a RemoteViews layout by calling updateAppWidget(int, RemoteViews):
            val views = RemoteViews(this.packageName, R.layout.new_app_widget)
            appWidManager.updateAppWidget(mAppWidgetId, views)
        }
    }
}
