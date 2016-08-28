package me.devsaki.hentoid.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.MenuItem;

import me.devsaki.hentoid.R;
import me.devsaki.hentoid.abstracts.BaseActivity;
import me.devsaki.hentoid.abstracts.BaseFragment;
import me.devsaki.hentoid.fragments.QueueFragment;
import me.devsaki.hentoid.util.LogHelper;

/**
 * Handles hosting of QueueFragment for a single screen.
 */
public class QueueActivity extends BaseActivity implements BaseFragment.BackInterface {
    private static final String TAG = LogHelper.makeLogTag(QueueActivity.class);

    private BaseFragment baseFragment;
    private Fragment fragment;

    private QueueFragment buildFragment() {
        return QueueFragment.newInstance();
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue);
        setTitle(R.string.title_activity_queue);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        FragmentManager manager = getSupportFragmentManager();
        fragment = manager.findFragmentById(R.id.content_frame);

        if (fragment == null) {
            fragment = buildFragment();

            manager.beginTransaction()
                    .add(R.id.content_frame, fragment, getFragmentTag())
                    .commit();
        }

        LogHelper.d(TAG, "onCreate");
    }

    private String getFragmentTag() {
        if (fragment != null) {
            return fragment.getClass().getSimpleName();
        }
        return null;
    }

    @Override
    public void onBackPressed() {
        if (baseFragment == null || baseFragment.onBackPressed()) {
            // Fragment did not consume onBackPressed.
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                super.onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void addBackInterface(BaseFragment fragment) {
        this.baseFragment = fragment;
    }
}
