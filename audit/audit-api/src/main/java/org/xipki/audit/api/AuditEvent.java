/*
 * Copyright 2014 xipki.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 *
 */

package org.xipki.audit.api;

import java.util.Arrays;
import java.util.Date;

public class AuditEvent extends AbstractAuditEvent
{
    /**
     * The name of the application the event belongs to.
     */
    protected String applicationName;

    /**
     * The data array belonging to the event.
     */
    protected AuditEventData[] eventDatas;


    /**
     * Default constructor for jaxb.
     */
    public AuditEvent()
    {
        // increment ID counter;
        id.getAndIncrement();
        setName(UNDEFINED);
        setApplicationName(UNDEFINED);
        setTimeStamp(new Date());
        setLevel(AuditLevel.INFO);
    }

    /**
     * Constructor for setting initial parameters.
     *
     * @param id
     *            Event id.
     * @param name
     *            Event name.
     * @param applicationName
     *            Application name.
     * @param timeStamp
     *            Timestamp when the event was saved.
     * @param eventDatas
     *            The event data array for this event.
     */
    public AuditEvent(final String name, final String applicationName, final Date timeStamp,
                 final AuditEventData[] eventDatas, final AuditLevel auditLevel)
    {
        id.getAndIncrement();
        setName(name);
        setApplicationName(applicationName);
        setTimeStamp(timeStamp);
        setEventDatas(eventDatas);
        setLevel(auditLevel);
    }

    public String getApplicationName()
    {
        return applicationName;
    }

    public void setApplicationName(final String applicationName)
    {
        this.applicationName = applicationName;
    }

    public AuditEventData[] getEventDatas()
    {
        return  Arrays.copyOf(eventDatas, eventDatas.length);
    }

    public void setEventDatas(final AuditEventData[] eventDataArray)
    {
        this.eventDatas =  Arrays.copyOf(eventDataArray, eventDataArray.length);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("AuditEvent ")
                .append("[applicationName=").append(applicationName)
                .append(", eventDatas=").append(Arrays.toString(eventDatas))
                .append(", id=").append(getId())
                .append(", name=").append(name)
                .append(", timeStamp=").append(timeStamp)
                .append(", level=").append(level).append("]");
        return builder.toString();
    }
}
