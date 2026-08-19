import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Concert } from '../../models/community.model';
import { CommunityService } from '../../services/community.service';
@Component({selector:'app-concerts',imports:[CommonModule,ReactiveFormsModule],templateUrl:'./concerts.component.html',styleUrl:'./concerts.component.css'})
export class ConcertsComponent {
  form=this.fb.group({artist:['',Validators.required],city:['']}); concerts:Concert[]=[]; loading=false; errorMessage=''; searched=false;
  constructor(private fb:FormBuilder,private service:CommunityService){}
  search():void{if(this.form.invalid)return;this.loading=true;this.errorMessage='';this.service.searchConcerts(this.form.value.artist||'',this.form.value.city||'').subscribe({next:v=>{this.concerts=v;this.loading=false;this.searched=true},error:e=>{this.loading=false;this.errorMessage=e.error?.message||'Could not search concerts'}});}
}
