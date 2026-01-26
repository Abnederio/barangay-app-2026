import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common'; // Import this for *ngIf
import { MyHttpClient } from '../my-http-client';

// Define the shape of data coming from Spring Boot
interface UserProfile {
  name: string;
  pictureUrl: string;
}

@Component({
  selector: 'app-private-content',
  standalone: true,
  imports: [CommonModule], // Add CommonModule here
  templateUrl: './private-content.html',
  styleUrl: './private-content.css',
})
export class PrivateContent implements OnInit {
  user: UserProfile | null = null;
  loading: boolean = true;

  constructor(private http: MyHttpClient) {}

  ngOnInit(): void {
    // We expect a UserProfile now, not just a Message
    this.http.get("/api/user/profile").subscribe({
      next: (data: any) => {
        this.user = {
          name: data.fullName || data.name || '',
          pictureUrl: data.pictureUrl || ''
        };
        this.loading = false;
      },
      error: (err: any) => {
        console.error("Error fetching profile", err);
        this.loading = false;
      }
    });
  }
}
