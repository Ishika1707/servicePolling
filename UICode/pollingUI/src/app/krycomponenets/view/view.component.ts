import { Component, OnInit } from '@angular/core';
import { ViewService } from './view.service';
import { Service } from 'src/app/krycomponenets/model/Service';

@Component({
  selector: 'app-comp1',
  templateUrl: './view.component.html',
  styleUrls: ['./view.component.css']
})
export class ViewComponent implements OnInit {

  columns = ["Name","Url","Status", "CreatedOn"];
  index = ["name", "url", "status", "date"];

  serviceList: Service[] = [];

  constructor(private service: ViewService) { }

  ngOnInit() {
  }

  getAllServices() {
    this.service.getAllServices().subscribe(result => {
      console.log(result);
      this.serviceList=result;
    });
  }

}
