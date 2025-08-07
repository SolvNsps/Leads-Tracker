package com.leadstracker.leadstracker.DTO;

import com.leadstracker.leadstracker.response.Statuses;

public class ClientStatusCountDto {
    private Statuses status;
    private Long count;

    public ClientStatusCountDto(Statuses status, Long count) {
        this.status = status;
        this.count = count;
    }

    public Statuses getStatus() {
        return status;
    }

    public void setStatus(Statuses status) {
        this.status = status;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
