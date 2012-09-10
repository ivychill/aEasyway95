package com.luyun.easyway95;

import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Put;

public interface UserProfileResource {

    @Get
	public UserProfile retrieve();

    @Put
    public void store(UserProfile profile);

    @Delete
    public void remove();
}

