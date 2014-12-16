package com.fairphone.updater.data;


public class Store extends DownloadableItem  implements Comparable<Store>
{
    private boolean mShowDisclaimer;
    
    public Store()
    {
        super();
        setShowDisclaimer(false);
    }
    
    public boolean showDisclaimer()
    {
        return mShowDisclaimer;
    }

    public void setShowDisclaimer(boolean showDisclaimer)
    {
        this.mShowDisclaimer = showDisclaimer;
    }

    @Override
    public int compareTo(Store another)
    {
        int retVal;
        if (another != null)
        {
            if (this.getNumber() > another.getNumber())
            {
                retVal = 1;
            }
            else if (this.getNumber() == another.getNumber())
            {
                retVal = 0;
            }
            else
            {
                retVal = -1;
            }
        }
        else
        {
            retVal = 1;
        }
        return retVal;
    }
    
}
