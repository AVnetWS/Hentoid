package me.devsaki.hentoid.fragments;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;

import java.util.List;

import me.devsaki.hentoid.R;
import me.devsaki.hentoid.abstracts.DownloadsFragment;
import me.devsaki.hentoid.adapters.ContentAdapter.ContentsWipedListener;
import me.devsaki.hentoid.adapters.ContentAdapter.EndlessScrollListener;
import me.devsaki.hentoid.database.domains.Content;
import me.devsaki.hentoid.util.ConstsPrefs;
import me.devsaki.hentoid.util.LogHelper;

/**
 * Created by avluis on 08/26/2016.
 * Presents the list of downloaded works to the user in an endless scroll list.
 */
public class EndlessFragment extends DownloadsFragment implements ContentsWipedListener,
        EndlessScrollListener {
    private static final String TAG = LogHelper.makeLogTag(EndlessFragment.class);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LogHelper.d(TAG, "onCreate: endless");
    }

    @Override
    protected void attachScrollListener() {
        mListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // Show toolbar:
                if (!override && result != null && result.size() > 0) {
                    // At top of list
                    if (llm.findViewByPosition(llm.findFirstVisibleItemPosition())
                            .getTop() == 0 && llm.findFirstVisibleItemPosition() == 0) {
                        showToolbar(true, false);
                        if (newContent) {
                            toolTip.setVisibility(View.VISIBLE);
                        }
                    }

                    // Last item in list
                    if (llm.findLastVisibleItemPosition() == result.size() - 1) {
                        showToolbar(true, false);
                    } else {
                        // When scrolling up
                        if (dy < -10) {
                            showToolbar(true, false);
                            if (newContent) {
                                toolTip.setVisibility(View.VISIBLE);
                            }
                            // When scrolling down
                        } else if (dy > 100) {
                            showToolbar(false, false);
                            if (newContent) {
                                toolTip.setVisibility(View.GONE);
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void attachRefresh(View rootView) {
        ImageButton btnRefresh = (ImageButton) rootView.findViewById(R.id.btnRefresh);
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isLoaded) {
                    update();
                }
            }
        });

        btnRefresh.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return false;
            }
        });
    }

    @Override
    protected void queryPrefs() {
        super.queryPrefs();

        qtyPages = ConstsPrefs.PREF_QUANTITY_PER_PAGE_DEFAULT;
    }

    @Override
    protected void checkResults() {
        if (contents != null) {
            LogHelper.d(TAG, "Contents are not null.");
        } else if (isLoaded && result != null) {
            LogHelper.d(TAG, "Result is not null.");
            result.clear();
        } else {
            LogHelper.d(TAG, "Contents are null.");
        }
        mAdapter.setEndlessScrollListener(this);

        if (result != null) {
            LogHelper.d(TAG, "Result is not null.");
            LogHelper.d(TAG, "Are results loaded? " + isLoaded);
            if (result.isEmpty() && !isLoaded) {
                LogHelper.d(TAG, "Result is empty!");
                update();
            }
            checkContent(false);
            mAdapter.setContentsWipedListener(this);
        } else {
            LogHelper.d(TAG, "Result is null.");

            update();
            checkContent(true);
        }

        if (!query.isEmpty()) {
            LogHelper.d(TAG, "Saved Query: " + query);
            update();
        }
    }

    @Override
    protected void showToolbar(boolean show, boolean override) {
        this.override = override;

        if (override) {
            if (show) {
                toolbar.setVisibility(View.VISIBLE);
            } else {
                toolbar.setVisibility(View.GONE);
            }
        } else {
            toolbar.setVisibility(View.GONE);
        }
    }

    @Override
    protected void displayResults() {
        //clearSelection();
        result = search.getContent();

        if (isLoaded) {
            toggleUI(0);
        }

        if (query.isEmpty()) {
            if (result != null && !result.isEmpty()) {

                if (contents == null) {
                    contents = result;
                    mAdapter.setContentList(contents);
                    mListView.setAdapter(mAdapter);
                } else {
                    int curSize = mAdapter.getItemCount();
                    contents.addAll(result);
                    mAdapter.notifyItemRangeInserted(curSize, contents.size() - 1);
                }

                toggleUI(SHOW_RESULT);
                updatePager();
            } else {
                LogHelper.d(TAG, "Result: Nothing to match.");
                displayNoResults();
            }
        } else {
            LogHelper.d(TAG, "Query: " + query);
            if (result != null && !result.isEmpty()) {
                LogHelper.d(TAG, "Result: Match.");

                List<Content> searchResults = result;
                mAdapter.setContentList(searchResults);
                mListView.setAdapter(mAdapter);

                toggleUI(SHOW_RESULT);
                showToolbar(true, true);
                updatePager();
            } else {
                LogHelper.d(TAG, "Result: Nothing to match.");
                displayNoResults();
            }
        }
    }

    @Override
    public void onLoadMore() {
        if (query.isEmpty()) {
            if (!isLastPage) {
                currentPage++;
                searchContent();
                LogHelper.d(TAG, "Load more data now~");
            }
        } else {
            LogHelper.d(TAG, "Endless Scrolling disabled.");
            mAdapter.enableFooter(false);
        }
    }
}
