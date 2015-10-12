package com.fairphone.updater.data;


public class Store extends DownloadableItem  implements Comparable<Store>
{
    private boolean mShowDisclaimer;
    
    public Store()
    {
        super();
        mShowDisclaimer = false;
    }

    public Store(Store other)
    {
        super(other);
        mShowDisclaimer = other.showDisclaimer();
    }
    
    public boolean showDisclaimer()
    {
        return mShowDisclaimer;
    }

    public void setShowDisclaimer()
    {
        this.mShowDisclaimer = true;
    }

    @Override
    public int compareTo(@SuppressWarnings("NullableProblems") Store another)
    {
        int retVal;
        if (another != null)
        {
            if (!this.getNumber().equals(another.getNumber()))
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
