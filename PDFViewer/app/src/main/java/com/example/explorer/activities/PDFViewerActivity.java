package com.example.explorer.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.explorer.R;
import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.config.ToolManagerBuilder;
import com.pdftron.pdf.controls.AddPageDialogFragment;
import com.pdftron.pdf.controls.AnnotationToolbar;
import com.pdftron.pdf.controls.AnnotationToolbarButtonId;
import com.pdftron.pdf.controls.FindTextOverlay;
import com.pdftron.pdf.controls.SearchToolbar;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.AppUtils;
import com.pdftron.pdf.utils.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static com.pdftron.pdf.controls.AnnotationToolbar.START_MODE_NORMAL_TOOLBAR;

public class PDFViewerActivity extends AppCompatActivity {
    private PDFViewCtrl mPdfViewCtrl;
    private PDFDoc mPdfDoc;
    private ToolManager mToolManager;
    private AnnotationToolbar mAnnotationToolbar;
    private ImageButton mSearchBtn;
    private ImageButton mSaveBtn;
    private ImageButton mAddPageBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdfviewer);

        Intent intent = getIntent();
        File fIle = (File)intent.getSerializableExtra("FileObj");

        mPdfViewCtrl = findViewById(R.id.pdfviewctrl);
        mSearchBtn = findViewById(R.id.search_btn);
        mSaveBtn = findViewById(R.id.save_btn);
        mAddPageBtn = findViewById(R.id.add_page_btn);

        setupToolManager();
        setupAnnotationToolbar();

        try {
            AppUtils.setupPDFViewCtrl(mPdfViewCtrl);
            viewFromLocalStorage(fIle);
        } catch (PDFNetException e) {
            // Handle exception
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        final SearchToolbar searchToolbar = findViewById(R.id.searchtoolbar);
        final FindTextOverlay searchOverlay = findViewById(R.id.find_text_view);
        searchOverlay.setPdfViewCtrl(mPdfViewCtrl);
        searchToolbar.setSearchToolbarListener(new SearchToolbar.SearchToolbarListener() {
            @Override
            public void onExitSearch() {
                searchToolbar.setVisibility(View.GONE);
                searchOverlay.setVisibility(View.GONE);
                searchOverlay.exitSearchMode();
            }

            @Override
            public void onClearSearchQuery() {
                searchOverlay.cancelFindText();
            }

            @Override
            public void onSearchQuerySubmit(String s) {
                searchOverlay.queryTextSubmit(s);
            }

            @Override
            public void onSearchQueryChange(String s) {
                searchOverlay.setSearchQuery(s);
            }

            @Override
            public void onSearchOptionsItemSelected(MenuItem menuItem, String s) {
                int id = menuItem.getItemId();
                if (id == R.id.action_match_case) {
                    boolean isChecked = menuItem.isChecked();
                    searchOverlay.setSearchMatchCase(!isChecked);
                    searchOverlay.resetFullTextResults();
                    menuItem.setChecked(!isChecked);
                } else if (id == R.id.action_whole_word) {
                    boolean isChecked = menuItem.isChecked();
                    searchOverlay.setSearchWholeWord(!isChecked);
                    searchOverlay.resetFullTextResults();
                    menuItem.setChecked(!isChecked);
                }
            }
        });

        mSearchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchToolbar.setVisibility(View.VISIBLE);
                searchOverlay.setVisibility(View.VISIBLE);
            }
        });

        mSaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean shouldUnlock = false;
                try {
                    mPdfViewCtrl.docLock(true);
                    shouldUnlock = true;
                    if (shouldUnlock) {
                        Toast.makeText(PDFViewerActivity.this,"저장하였습니다.", Toast.LENGTH_LONG).show();
                        mPdfDoc.save(); // save incrementally
                    }
                } catch (Exception e) {
                    Toast.makeText(PDFViewerActivity.this,"에러가 발생하여 저장하지 못하였습니다.", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                } finally {
                    if (shouldUnlock) {
                        mPdfViewCtrl.docUnlock();
                    }
                }
            }
        });

        mAddPageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addPagesToCurrentDocument(getSupportFragmentManager(),mPdfViewCtrl);
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mPdfViewCtrl != null) {
            mPdfViewCtrl.pause();
            mPdfViewCtrl.purgeMemory();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mPdfViewCtrl != null) {
            mPdfViewCtrl.resume();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPdfViewCtrl != null) {
            mPdfViewCtrl.destroy();
            mPdfViewCtrl = null;
        }

        if (mPdfDoc != null) {
            try {
                mPdfDoc.close();
            } catch (Exception e) {
                // handle exception
            } finally {
                mPdfDoc = null;
            }
        }
    }

    /**
     * Helper method to set up and initialize the ToolManager.
     */
    public void setupToolManager() {
        mToolManager = ToolManagerBuilder.from()
                .build(this, mPdfViewCtrl);
        mToolManager.disableToolMode(new ToolManager.ToolMode[]{
                ToolManager.ToolMode.DIGITAL_SIGNATURE,
                ToolManager.ToolMode.FREE_HIGHLIGHTER,
                ToolManager.ToolMode.LINE_CREATE,
                ToolManager.ToolMode.RECT_CREATE,
                ToolManager.ToolMode.SIGNATURE,
                ToolManager.ToolMode.ARROW_CREATE,
                ToolManager.ToolMode.OVAL_CREATE,
                ToolManager.ToolMode.TEXT_SQUIGGLY,
                ToolManager.ToolMode.POLYGON_CREATE,
                ToolManager.ToolMode.POLYLINE_CREATE,
                ToolManager.ToolMode.RULER_CREATE,
                ToolManager.ToolMode.CLOUD_CREATE,
                ToolManager.ToolMode.PERIMETER_MEASURE_CREATE,
                ToolManager.ToolMode.AREA_MEASURE_CREATE,
                ToolManager.ToolMode.RUBBER_STAMPER,
                ToolManager.ToolMode.TEXT_ANNOT_CREATE,
                ToolManager.ToolMode.SOUND_CREATE,
                ToolManager.ToolMode.STAMPER

        });
        mToolManager.setAnnotToolbarPrecedence(new ToolManager.ToolMode[] {
                ToolManager.ToolMode.INK_CREATE,
                ToolManager.ToolMode.INK_ERASER,
                ToolManager.ToolMode.TEXT_HIGHLIGHT,
                ToolManager.ToolMode.TEXT_UNDERLINE,
                ToolManager.ToolMode.TEXT_STRIKEOUT,
                ToolManager.ToolMode.TEXT_CREATE

        });
    }

    /**
     * Helper method to set up and initialize the AnnotationToolbar.
     */
    public void setupAnnotationToolbar() {
        mAnnotationToolbar = findViewById(R.id.annotationToolbar);
        // Remember to initialize your ToolManager before calling setup
        mAnnotationToolbar.setup(mToolManager);
        mAnnotationToolbar.hideButton(AnnotationToolbarButtonId.CLOSE);
        mAnnotationToolbar.setButtonStayDown(true);
        mAnnotationToolbar.show(START_MODE_NORMAL_TOOLBAR);
    }


    public void viewFromLocalStorage(File file) throws PDFNetException, FileNotFoundException {
        // Alternatively, you can open the document using Uri:
         Uri fileUri = Uri.fromFile(file);
         mPdfDoc = mPdfViewCtrl.openPDFUri(fileUri, null);
    }

    void addPagesToCurrentDocument(FragmentManager fragmentManager, PDFViewCtrl pdfViewCtrl) {
        boolean shouldUnlockRead = false;
        final WeakReference<PDFViewCtrl> pdfViewCtrlRef = new WeakReference<>(pdfViewCtrl);
        try {
            pdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            // enable user to add new pages with the same size as the last page of the current document
            Page lastPage = pdfViewCtrl.getDoc().getPage(pdfViewCtrl.getDoc().getPageCount());
            AddPageDialogFragment addPageDialogFragment = AddPageDialogFragment.newInstance(lastPage.getPageWidth(), lastPage.getPageHeight());
            addPageDialogFragment.setOnAddNewPagesListener(new AddPageDialogFragment.OnAddNewPagesListener() {
                @Override
                public void onAddNewPages(Page[] pages) {
                    PDFViewCtrl pdfViewCtrl = pdfViewCtrlRef.get();
                    if (pages == null || pdfViewCtrl == null) {
                        return;
                    }
                    PDFDoc doc = pdfViewCtrl.getDoc();
                    if (doc == null) {
                        return;
                    }

                    boolean shouldUnlock = false;
                    try {
                        pdfViewCtrl.docLock(true);
                        shouldUnlock = true;
                        List<Integer> pageList = new ArrayList<>();
                        for (int i = 1, cnt = pages.length; i <= cnt; i++) {
                            int newPageNum = pdfViewCtrl.getCurrentPage() + i;
                            pageList.add(newPageNum);
                            doc.pageInsert(doc.getPageIterator(newPageNum), pages[i - 1]);
                        }

                        // To support undo/redo when a tool manager is attached to the PDFViewCtrl
                        ToolManager toolManager = (ToolManager) pdfViewCtrl.getToolManager();
                        if (toolManager != null) {
                            toolManager.raisePagesAdded(pageList);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (shouldUnlock) {
                            pdfViewCtrl.docUnlock();
                        }
                        try {
                            pdfViewCtrl.updatePageLayout();
                        } catch (PDFNetException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            addPageDialogFragment.show(fragmentManager, "add_page_dialog");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (shouldUnlockRead) {
                pdfViewCtrl.docUnlockRead();
            }
        }
    }
}
