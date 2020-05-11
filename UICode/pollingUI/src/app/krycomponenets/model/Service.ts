export class Service {
    name: String;
    url: String;
    status: String;
    date: String;

    constructor (name, url, status, date) {
        this.name=name;
        this.url=url;
        this.status=status;
        this.date=date;
    }
}