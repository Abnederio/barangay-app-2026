import { Component, OnInit } from '@angular/core';
import { MyHttpClient } from '../my-http-client';
import { Message } from '../message';

@Component({
  selector: 'app-public-content',
  standalone: true, // Ensuring modern standalone support
  templateUrl: './public-content.html',
  styleUrl: './public-content.css',
})
export class PublicContent implements OnInit {
  content: string = "";

  constructor(private http: MyHttpClient) {}

  ngOnInit(): void {
    // Calling your Spring Boot @GetMapping("/public/messages")
    this.http.get("/public/messages").subscribe((data: Message) => {
      this.content = data.message;
    });
  }
}
